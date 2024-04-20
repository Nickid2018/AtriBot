package io.github.nickid2018.atribot.plugins.wiki;

import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.message.CommandCommunicateData;
import io.github.nickid2018.atribot.core.message.MessageManager;
import io.github.nickid2018.atribot.network.message.MessageChain;
import io.github.nickid2018.atribot.network.message.TargetData;
import io.github.nickid2018.atribot.plugins.wiki.persist.StartWikiEntry;
import io.github.nickid2018.atribot.plugins.wiki.persist.WikiEntry;
import io.github.nickid2018.atribot.plugins.wiki.resolve.InterwikiStorage;
import io.github.nickid2018.atribot.plugins.wiki.resolve.PageInfo;
import io.github.nickid2018.atribot.plugins.wiki.resolve.WikiInfo;
import io.github.nickid2018.atribot.util.FunctionUtil;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@AllArgsConstructor
public class WikiResolver implements CommunicateReceiver {

    private static final Set<String> KEYS = Set.of(
        "atribot.message.command",
        "atribot.message.normal"
    );

    private final WikiPlugin plugin;
    private final InterwikiStorage interwikiStorage;

    @Override
    public <T, D> CompletableFuture<T> communicate(String communicateKey, D data) throws Exception {
        if (communicateKey.equals("atribot.message.command")) {
            CommandCommunicateData commandData = (CommandCommunicateData) data;
            switch (commandData.commandHead) {
                case "wiki" -> plugin.getExecutorService().execute(noException(() -> requestWikiPage(
                    commandData.commandArgs,
                    commandData.backendID,
                    commandData.targetData,
                    commandData.messageManager
                ), commandData.backendID, commandData.targetData, commandData.messageManager));
                case "wikiadd" -> plugin.getExecutorService().execute(noException(() -> addWiki(
                    commandData.commandArgs,
                    commandData.backendID,
                    commandData.targetData,
                    commandData.messageManager
                ), commandData.backendID, commandData.targetData, commandData.messageManager));
                case "wikistart" -> plugin.getExecutorService().execute(noException(() -> setStartWiki(
                    commandData.commandArgs,
                    commandData.backendID,
                    commandData.targetData,
                    commandData.messageManager
                ), commandData.backendID, commandData.targetData, commandData.messageManager));
                default -> {
                }
            }
        }
        return null;
    }

    private Runnable noException(FunctionUtil.RunnableWithException<? extends Throwable> runnable,
        String backendID, TargetData targetData, MessageManager manager) {
        return FunctionUtil.noExceptionOrElse(
            runnable,
            e -> {
                manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：查询页面时出现错误"));
                log.error("Error when querying wiki page", e);
            }
        );
    }

    @SneakyThrows
    private void addWiki(String[] args, String backendID, TargetData targetData, MessageManager manager) {
        if (args.length < 2) {
            manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：未指定 Wiki 名称或 URL"));
            return;
        }

        String wikiName = args[0];
        String wikiURL = args[1];
        WikiEntry entry = new WikiEntry();
        entry.group = targetData.isGroupMessage() ? targetData.getTargetGroup() : targetData.getTargetUser();
        entry.wikiPrefix = wikiName;
        entry.baseURL = wikiURL;

        plugin.wikiEntries
            .queryForEq("group", entry.group)
            .stream()
            .filter(en -> en.wikiPrefix.equals(wikiName))
            .findFirst()
            .ifPresent(FunctionUtil.<WikiEntry>noException(plugin.wikiEntries::delete));
        plugin.wikiEntries.create(entry);
        interwikiStorage.addWiki(wikiURL);

        manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：添加 Wiki 成功"));
    }

    @SneakyThrows
    private void setStartWiki(String[] args, String backendID, TargetData targetData, MessageManager manager) {
        if (args.length < 1) {
            manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：未指定 Wiki 名称"));
            return;
        }

        String wikiName = args[0];
        boolean containsName = plugin.wikiEntries
            .queryForEq("group", targetData.isGroupMessage() ? targetData.getTargetGroup() : targetData.getTargetUser())
            .stream()
            .anyMatch(entry -> entry.wikiPrefix.equals(wikiName));
        if (!containsName) {
            manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：未找到对应 Wiki"));
            return;
        }

        StartWikiEntry startWiki = new StartWikiEntry();
        startWiki.group = targetData.isGroupMessage() ? targetData.getTargetGroup() : targetData.getTargetUser();
        startWiki.wikiKey = wikiName;
        plugin.startWikis.create(startWiki);

        manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：起始 Wiki 设置成功"));
    }

    @SneakyThrows
    private void requestWikiPage(String[] args, String backendID, TargetData targetData, MessageManager manager) {
        if (args.length == 0) {
            manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：未指定查询页面"));
            return;
        }

        String searchTitle = String.join(" ", args);
        int sectionSplit = searchTitle.indexOf('#');
        String section = null;
        if (sectionSplit >= 0) {
            section = searchTitle.substring(sectionSplit + 1);
            searchTitle = searchTitle.substring(0, sectionSplit);
        }
        String[] interwikiSplits = searchTitle.split(":");
        List<String> interwikiSplitsList = new ArrayList<>(Arrays.asList(interwikiSplits));

        String target = targetData.isGroupMessage() ? targetData.getTargetGroup() : targetData.getTargetUser();
        StartWikiEntry startWiki = plugin.startWikis.queryForId(target);
        String wikiKey = startWiki == null ? null : startWiki.wikiKey;
        List<WikiEntry> wikiEntries = plugin.wikiEntries.queryForEq("group", target);
        WikiEntry startWikiEntry = wikiEntries
            .stream()
            .filter(entry -> entry.wikiPrefix.equals(wikiKey))
            .findFirst()
            .orElse(null);
        Optional<WikiEntry> nowWikiEntry = wikiEntries
            .stream()
            .filter(entry -> entry.wikiPrefix.equals(interwikiSplitsList.getFirst()))
            .findFirst()
            .filter(en -> interwikiSplitsList.size() > 1);
        WikiEntry wikiEntry = nowWikiEntry.orElse(startWikiEntry);
        nowWikiEntry.ifPresent(en -> interwikiSplitsList.removeFirst());

        if (wikiEntry == null) {
            manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：未找到对应 Wiki"));
            return;
        }

        PageInfo lastFoundPageInfo = null;
        String lastInterwiki = "";
        for (int i = 0; i < interwikiSplitsList.size(); i++) {
            String interwiki = String.join(":", interwikiSplitsList.subList(0, i));
            String namespace = i + 1 >= interwikiSplitsList.size() ? null : interwikiSplitsList.get(i);
            String title = i + 1 >= interwikiSplitsList.size() ? interwikiSplitsList.get(i) : String.join(
                ":",
                interwikiSplitsList.subList(i + 1, interwikiSplitsList.size())
            );

            WikiInfo wikiInfo = interwikiStorage.getWikiInfo(wikiEntry.baseURL, interwiki).get();
            if (wikiInfo == null)
                break;
            lastFoundPageInfo = wikiInfo.parsePageInfo(namespace, title, section);
            lastInterwiki = interwiki;
        }

        if (!lastInterwiki.isEmpty())
            lastInterwiki += ":";
        if (nowWikiEntry.isPresent())
            lastInterwiki = interwikiSplits[0] + ":" + lastInterwiki;

        if (lastFoundPageInfo == null) {
            manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：未找到页面"));
            return;
        }

        processPageInfo(lastFoundPageInfo, lastInterwiki, "", backendID, targetData, manager);
    }

    private void processPageInfo(PageInfo info, String interwiki, String prependStr, String backendID, TargetData targetData, MessageManager manager) {
        Map<String, Object> data = info.data();
        StringBuilder message = new StringBuilder(prependStr);
        switch (info.type()) {
            case NORMAL -> {
                message.append(data.get("content"));
                message.append("\n");
                message.append(data.get("pageURL"));
            }
            case REDIRECT -> {
                if ((boolean) data.get("multipleRedirects"))
                    message.append("[注意：此页面被多重重定向！]\n");
                message.append("（[[");
                message.append(interwiki);
                message.append(data.get("pageNameSource"));
                message.append("]] 重定向至 [[");
                message.append(interwiki);
                message.append(data.get("pageNameRedirected"));
                message.append("]]）\n");
                PageInfo pageInfo = (PageInfo) data.get("pageInfo");
                processPageInfo(pageInfo, interwiki, message.toString(), backendID, targetData, manager);
                return;
            }
            case NORMALIZED -> {
                message.append("（[[");
                message.append(interwiki);
                message.append(data.get("pageNameSource"));
                message.append("]] 自动修正为 [[");
                message.append(interwiki);
                message.append(data.get("pageNameRedirected"));
                message.append("]]）\n");
                PageInfo pageInfo = (PageInfo) data.get("pageInfo");
                processPageInfo(pageInfo, interwiki, message.toString(), backendID, targetData, manager);
                return;
            }
            case SPECIAL -> {
                message.append("特殊页面\n");
                message.append(data.get("pageURL"));
            }
            case DIRECT_URL -> {
                message.append("目标为一个 URL：");
                message.append(data.get("url"));
            }
            case DISAMBIGUATION -> {
                message.append("消歧义页面\n");
                message.append(data.get("pageURL"));
            }
            case ANONYMOUS_USER_PAGE -> message.append("匿名用户页面");
            case UNSUPPORTED -> {
                message.append("暂不支持的页面类型\n");
                message.append(data.get("pageURL"));
            }
            case NETWORK_ERROR -> {
                message.append("Wiki：查询页面过程中出现错误，报告信息如下\n");
                message.append(((Throwable) data.get("error")).getMessage());
            }
            case PAGE_NOT_FOUND -> {
                message.append("没有找到 [[");
                message.append(interwiki);
                message.append(data.get("pageName"));
                message.append("]] ，下列为搜索得到的类似页面：\n");
                String[] searchTitles = (String[]) data.get("pageSuggestions");
                message.append(String.join("\n", searchTitles));
            }
            case SECTION_NOT_FOUND -> {
                message.append("没有找到页面 [[");
                message.append(interwiki);
                message.append(data.get("pageName"));
                message.append("]] 中的指定章节，下列为页面内存在的所有章节：\n");
                String[] availableSections = (String[]) data.get("availableSections");
                message.append(String.join("\n", availableSections));
                message.append("\n");
                message.append(data.get("pageURL"));
            }
        }

        manager.sendMessage(backendID, targetData, MessageChain.text(message.toString()));
    }

    @Override
    public Set<String> availableCommunicateKeys() {
        return KEYS;
    }
}
