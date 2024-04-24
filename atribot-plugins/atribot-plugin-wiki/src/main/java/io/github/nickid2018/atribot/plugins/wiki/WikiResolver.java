package io.github.nickid2018.atribot.plugins.wiki;

import com.j256.ormlite.stmt.DeleteBuilder;
import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.message.CommandCommunicateData;
import io.github.nickid2018.atribot.core.message.MessageManager;
import io.github.nickid2018.atribot.core.message.PermissionLevel;
import io.github.nickid2018.atribot.network.message.ImageMessage;
import io.github.nickid2018.atribot.network.message.MessageChain;
import io.github.nickid2018.atribot.network.message.TargetData;
import io.github.nickid2018.atribot.plugins.wiki.persist.StartWikiEntry;
import io.github.nickid2018.atribot.plugins.wiki.persist.WikiEntry;
import io.github.nickid2018.atribot.plugins.wiki.resolve.*;
import io.github.nickid2018.atribot.util.FunctionUtil;
import it.unimi.dsi.fastutil.Pair;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Slf4j
@AllArgsConstructor
public class WikiResolver implements CommunicateReceiver {

    private static final Set<String> KEYS = Set.of("atribot.message.command", "atribot.message.normal");

    private final WikiPlugin plugin;
    private final InterwikiStorage interwikiStorage;

    @Override
    public <T, D> CompletableFuture<T> communicate(String communicateKey, D data) {
        if (communicateKey.equals("atribot.message.command")) {
            CommandCommunicateData commandData = (CommandCommunicateData) data;
            switch (commandData.commandHead) {
                case "wiki" -> executeWikiCommand(commandData, this::requestWikiPage);
                case "wikiadd" -> executeWikiCommand(commandData, this::addWiki);
                case "wikistart" -> executeWikiCommand(commandData, this::setStartWiki);
                case "wikiremove" -> executeWikiCommand(commandData, this::removeWiki);
                case "wikirender" -> executeWikiCommand(commandData, this::setWikiRendering);
                case "wikirandom" -> executeWikiCommand(commandData, this::randomPage);
                default -> {
                }
            }
        }
        return null;
    }

    private interface WikiCommand {
        void execute(String[] args, String backendID, TargetData targetData, MessageManager manager);
    }

    private void executeWikiCommand(CommandCommunicateData data, WikiCommand command) {
        plugin.getExecutorService().execute(noException(
            () -> command.execute(
                data.commandArgs,
                data.backendID,
                data.targetData,
                data.messageManager
            ),
            data.backendID,
            data.targetData,
            data.messageManager
        ));
    }

    private Runnable noException(FunctionUtil.RunnableWithException<? extends Throwable> runnable, String backendID, TargetData targetData, MessageManager manager) {
        return FunctionUtil.tryOrElse(runnable, e -> {
            manager.sendMessage(
                backendID,
                targetData,
                MessageChain.text("Wiki：操作时出现错误，错误报告如下\n" + e.getMessage())
            );
            log.error("Error when querying wiki page", e);
        });
    }

    @SneakyThrows
    private void addWiki(String[] args, String backendID, TargetData targetData, MessageManager manager) {
        if (!manager.getPermissionManager().checkTargetData(targetData, PermissionLevel.ADMIN)) {
            manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：权限不足"));
            return;
        }

        if (args.length < 2) {
            manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：未指定 Wiki 名称或 URL"));
            return;
        }

        String group = targetData.isGroupMessage() ? targetData.getTargetGroup() : targetData.getTargetUser();
        String wikiName = args[0];
        String wikiURL = args[1];
        WikiEntry entry = new WikiEntry();
        entry.group = group;
        entry.wikiPrefix = wikiName;
        entry.baseURL = wikiURL;

        DeleteBuilder<WikiEntry, Object> deleteBuilder = plugin.wikiEntries.deleteBuilder();
        deleteBuilder.where().eq("group", group).and().eq("wiki_key", args[0]);
        deleteBuilder.delete();
        plugin.wikiEntries.create(entry);
        interwikiStorage.addWiki(wikiURL);

        manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：添加 Wiki 成功"));
    }

    @SneakyThrows
    private void setStartWiki(String[] args, String backendID, TargetData targetData, MessageManager manager) {
        if (!manager.getPermissionManager().checkTargetData(targetData, PermissionLevel.ADMIN)) {
            manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：权限不足"));
            return;
        }

        if (args.length < 1) {
            manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：未指定 Wiki 名称"));
            return;
        }

        String wikiName = args[0];
        String group = targetData.isGroupMessage() ? targetData.getTargetGroup() : targetData.getTargetUser();
        boolean containsName = plugin.wikiEntries
            .queryForEq("group", group)
            .stream()
            .anyMatch(entry -> entry.wikiPrefix.equals(wikiName));
        if (!containsName) {
            manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：未找到对应 Wiki"));
            return;
        }

        StartWikiEntry startWiki = new StartWikiEntry();
        startWiki.group = group;
        startWiki.wikiKey = wikiName;
        plugin.startWikis.create(startWiki);

        manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：起始 Wiki 设置成功"));
    }

    @SneakyThrows
    private void removeWiki(String[] args, String backendID, TargetData targetData, MessageManager manager) {
        if (!manager.getPermissionManager().checkTargetData(targetData, PermissionLevel.ADMIN)) {
            manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：权限不足"));
            return;
        }

        if (args.length < 1) {
            manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：未指定 Wiki 名称"));
            return;
        }

        String group = targetData.isGroupMessage() ? targetData.getTargetGroup() : targetData.getTargetUser();
        StartWikiEntry startWiki = plugin.startWikis.queryForId(group);
        if (startWiki != null && startWiki.wikiKey.equals(args[0]))
            plugin.startWikis.delete(startWiki);

        DeleteBuilder<WikiEntry, Object> deleteBuilder = plugin.wikiEntries.deleteBuilder();
        deleteBuilder.where().eq("group", group).and().eq("wiki_key", args[0]);
        deleteBuilder.delete();

        manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：移除 Wiki 成功"));
    }

    @SneakyThrows
    private void setWikiRendering(String[] args, String backendID, TargetData targetData, MessageManager manager) {
        if (!manager.getPermissionManager().checkTargetData(targetData, PermissionLevel.SUPER_USER)) {
            manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：权限不足"));
            return;
        }

        if (args.length < 2) {
            manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：未指定 Wiki 名称或参数"));
            return;
        }

        String wikiName = args[0];
        String group = targetData.isGroupMessage() ? targetData.getTargetGroup() : targetData.getTargetUser();
        Optional<WikiEntry> wikiEntry = plugin.wikiEntries
            .queryForEq("group", group)
            .stream()
            .filter(entry -> entry.wikiPrefix.equals(wikiName))
            .findFirst();
        if (wikiEntry.isEmpty()) {
            manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：未找到对应 Wiki"));
            return;
        }

        boolean makeRender = Boolean.parseBoolean(args[1]);
        WikiEntry entry = wikiEntry.get();
        entry.trustRender = makeRender;

        DeleteBuilder<WikiEntry, Object> deleteBuilder = plugin.wikiEntries.deleteBuilder();
        deleteBuilder.where().eq("group", group).and().eq("wiki_key", args[0]);
        deleteBuilder.delete();
        plugin.wikiEntries.create(entry);

        manager.sendMessage(
            backendID,
            targetData,
            MessageChain.text("Wiki：已" + (makeRender ? "允许 " : "禁止 ") + args[0] + " 渲染")
        );
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
        WikiInfo lastWikiInfo = null;
        for (int i = 0; i < interwikiSplitsList.size(); i++) {
            String interwiki = String.join(":", interwikiSplitsList.subList(0, i));
            String namespace = i + 1 >= interwikiSplitsList.size() ? null : interwikiSplitsList.get(i);
            String title = i + 1 >= interwikiSplitsList.size() ? interwikiSplitsList.get(i) : String.join(
                ":",
                interwikiSplitsList.subList(
                    i + 1,
                    interwikiSplitsList.size()
                )
            );

            WikiInfo wikiInfo = interwikiStorage.getWikiInfo(wikiEntry.baseURL, interwiki).get();
            if (wikiInfo == null)
                break;
            lastWikiInfo = wikiInfo;
            lastFoundPageInfo = wikiInfo.parsePageInfo(namespace, title, section);
            lastInterwiki = interwiki;
            if (lastFoundPageInfo.type() == PageType.PAGE_NOT_FOUND && namespace != null)
                lastFoundPageInfo = wikiInfo.parsePageInfo(null, namespace + ":" + title, section);
        }

        if (!lastInterwiki.isEmpty())
            lastInterwiki += ":";
        if (nowWikiEntry.isPresent())
            lastInterwiki = interwikiSplits[0] + ":" + lastInterwiki;

        if (lastFoundPageInfo == null) {
            manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：未找到页面"));
            return;
        }

        processPageInfo(
            lastFoundPageInfo,
            lastWikiInfo,
            lastInterwiki,
            "",
            backendID,
            targetData,
            manager,
            wikiEntry
        );
    }

    @SneakyThrows
    private void randomPage(String[] args, String backendID, TargetData targetData, MessageManager manager) {
        if (args.length > 1) {
            manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：参数过多"));
            return;
        }

        String target = targetData.isGroupMessage() ? targetData.getTargetGroup() : targetData.getTargetUser();
        StartWikiEntry startWiki = plugin.startWikis.queryForId(target);
        String wikiKey = startWiki == null ? null : startWiki.wikiKey;

        WikiInfo wikiInfo = null;
        List<WikiEntry> wikiEntries = plugin.wikiEntries.queryForEq("group", target);
        WikiEntry startWikiEntry = wikiEntries
            .stream()
            .filter(entry -> entry.wikiPrefix.equals(wikiKey))
            .findFirst()
            .orElse(null);
        if (args.length == 1) {
            if (startWikiEntry != null) {
                wikiInfo = interwikiStorage.getWikiInfo(startWikiEntry.baseURL, args[0]).get();
            }
            if (wikiInfo == null) {
                int firstInterwikiSplit = args[0].indexOf(':');
                if (firstInterwikiSplit < 0)
                    firstInterwikiSplit = args[0].length();
                String first = args[0].substring(0, firstInterwikiSplit);
                WikiEntry nowWikiEntry = wikiEntries
                    .stream()
                    .filter(entry -> entry.wikiPrefix.equals(first))
                    .findFirst()
                    .orElse(null);
                if (nowWikiEntry == null) {
                    manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：未找到对应 Wiki"));
                    return;
                }
                String interwiki = firstInterwikiSplit == args[0].length() ? "" : args[0].substring(firstInterwikiSplit + 1);
                wikiInfo = interwikiStorage.getWikiInfo(nowWikiEntry.baseURL, interwiki).get();
            }
        } else {
            if (startWikiEntry == null) {
                manager.sendMessage(backendID, targetData, MessageChain.text("Wiki：未指定起始 Wiki"));
                return;
            }
            wikiInfo = interwikiStorage.getWikiInfo(startWikiEntry.baseURL, "").get();
        }

        String randomPage = wikiInfo.randomPage();
        PageInfo pageInfo = wikiInfo.parsePageInfo(null, randomPage, null);
        String interwiki = args.length == 1 ? args[0] + ":" : "";
        processPageInfo(
            pageInfo,
            wikiInfo,
            interwiki,
            "（随机页面到 [[" + interwiki + randomPage + "]]）\n",
            backendID,
            targetData,
            manager,
            startWikiEntry
        );
    }

    @SuppressWarnings("unchecked")
    private void processPageInfo(PageInfo info, WikiInfo wikiInfo, String interwiki,
        String prependStr, String backendID, TargetData targetData, MessageManager manager, WikiEntry wikiEntry) {
        Map<String, Object> data = info.data();
        StringBuilder message = new StringBuilder(prependStr);
        switch (info.type()) {
            case NORMAL -> {
                message.append(data.get("content"));
                message.append("\n");
                message.append(data.get("pageURL"));
            }
            case WITH_DOCUMENT -> {
                if ((boolean) data.get("hasDoc"))
                    message.append(data.get("desc"));
                else
                    message.append("[注意：此页面没有文档！]");
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
                processPageInfo(
                    pageInfo,
                    wikiInfo,
                    interwiki,
                    message.toString(),
                    backendID,
                    targetData,
                    manager,
                    wikiEntry
                );
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
                processPageInfo(
                    pageInfo,
                    wikiInfo,
                    interwiki,
                    message.toString(),
                    backendID,
                    targetData,
                    manager,
                    wikiEntry
                );
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
            case FILES -> {
                List<Pair<String, String>> fileAndMIME = (List<Pair<String, String>>) data.get("fileAndMIME");
                message.append("文件页面\n");
                message.append(data.get("pageURL"));
                message.append("\n共含有 ");
                message.append(fileAndMIME.size());
                message.append(" 个文件：");
                for (Pair<String, String> pair : fileAndMIME) {
                    message.append("\n");
                    message.append(pair.left());
                    message.append(" （MIME 类型：");
                    message.append(pair.right());
                    message.append("）");
                }
            }
            case ANONYMOUS_USER_PAGE -> message.append("匿名用户页面");
            case UNSUPPORTED -> {
                message.append("暂不支持的页面类型\n");
                message.append(data.get("pageURL"));
            }
            case NETWORK_ERROR -> {
                Throwable error = (Throwable) data.get("error");
                message.append("Wiki：查询页面过程中出现错误，报告信息如下\n");
                message.append(error.getMessage());
                log.error("Error when querying wiki page", error);
            }
            case PAGE_NOT_FOUND -> {
                message.append("没有找到 [[");
                message.append(interwiki);
                message.append(data.get("pageName"));
                message.append("]] ，搜索得到的最相似页面为：\n");
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

        if (!wikiEntry.trustRender)
            return;

        doRender(
            switch (info.type()) {
                case NORMAL -> (Function<PageShooter, CompletableFuture<byte[]>>) (shooter -> {
                    String pageSection = (String) data.get("pageSection");
                    if (pageSection == null || pageSection.isEmpty())
                        return shooter.renderInfobox(plugin, wikiEntry);
                    else
                        return shooter.renderSection(
                            plugin,
                            (String) data.get("pageName"),
                            (String) data.get("pageSectionIndex"),
                            wikiInfo,
                            wikiEntry
                        );
                });
                case DISAMBIGUATION ->
                    (Function<PageShooter, CompletableFuture<byte[]>>) (shooter -> shooter.renderFullPage(
                        plugin,
                        wikiEntry
                    ));
                case WITH_DOCUMENT -> (boolean) data.get("hasDoc") ?
                                      (Function<PageShooter, CompletableFuture<byte[]>>) (shooter -> shooter.renderDoc(
                                          plugin,
                                          wikiEntry
                                      )) :
                                      null;
                case SPECIAL -> (Function<PageShooter, CompletableFuture<byte[]>>) (shooter -> shooter.renderSpecials(
                    plugin,
                    wikiEntry
                ));
                default -> null;
            },
            (String) data.get("pageURL"),
            backendID,
            targetData,
            manager
        );

        if (info.type() == PageType.FILES) {
            List<Pair<String, String>> fileAndMIME = (List<Pair<String, String>>) data.get("fileAndMIME");
            for (Pair<String, String> pair : fileAndMIME) {
                String fileURL = pair.left();
                String MIME = pair.right();
                if (MIME.startsWith("image/")) {
                    manager.sendMessage(
                        backendID,
                        targetData,
                        new MessageChain().next(new ImageMessage("", URI.create(fileURL)))
                    );
                }
            }
        }
    }

    private void doRender(Function<PageShooter, CompletableFuture<byte[]>> screenshotFunc, String pageURL,
        String backendID, TargetData targetData, MessageManager manager) {
        if (screenshotFunc == null)
            return;
        CompletableFuture
            .supplyAsync(() -> new PageShooter(pageURL), plugin.getExecutorService())
            .thenCompose(screenshotFunc)
            .thenComposeAsync(image -> {
                if (image == null)
                    return CompletableFuture.completedFuture(null);
                return manager.getFileTransfer().sendFile(
                    backendID,
                    new ByteArrayInputStream(image),
                    plugin.getExecutorService()
                );
            }, plugin.getExecutorService())
            .thenAccept(fileID -> {
                if (fileID == null)
                    return;
                manager.sendMessage(
                    backendID,
                    targetData,
                    new MessageChain().next(new ImageMessage("", URI.create(fileID)))
                );
            })
            .exceptionallyAsync(e -> {
                String errorMessage = e.getMessage();
                if (errorMessage.contains("NS_ERROR_FAILURE")) {
                    manager.sendMessage(
                        backendID,
                        targetData,
                        MessageChain.text("Wiki：页面可能过长，无法渲染")
                    );
                    log.debug("Error when rendering wiki page, may page is too long?", e);
                } else {
                    errorMessage = errorMessage.length() > 200 ? errorMessage.substring(0, 200) + "..." : errorMessage;
                    manager.sendMessage(
                        backendID,
                        targetData,
                        MessageChain.text("Wiki：渲染页面时出现错误，错误报告如下\n" + errorMessage)
                    );
                    log.error("Error when rendering wiki page", e);
                }
                return null;
            }, plugin.getExecutorService());
    }

    @Override
    public Set<String> availableCommunicateKeys() {
        return KEYS;
    }
}
