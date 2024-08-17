package io.github.nickid2018.atribot.plugins.bugtracker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.atribot.core.communicate.*;
import io.github.nickid2018.atribot.core.message.CommandCommunicateData;
import io.github.nickid2018.atribot.core.message.MessageManager;
import io.github.nickid2018.atribot.core.plugin.AbstractAtriBotPlugin;
import io.github.nickid2018.atribot.core.plugin.PluginInfo;
import io.github.nickid2018.atribot.network.message.ImageMessage;
import io.github.nickid2018.atribot.network.message.MessageChain;
import io.github.nickid2018.atribot.network.message.TargetData;
import io.github.nickid2018.atribot.network.message.TextMessage;
import io.github.nickid2018.atribot.util.JsonUtil;
import io.github.nickid2018.atribot.util.WebUtil;
import lombok.SneakyThrows;
import org.apache.hc.client5.http.classic.methods.HttpGet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class BugTrackerPlugin extends AbstractAtriBotPlugin implements CommunicateReceiver {
    @Override
    public PluginInfo getPluginInfo() {
        return new PluginInfo(
            "atribot-plugin-bugtracker",
            "BugTracker",
            "1.0",
            "Nickid2018",
            "A plugin for bug tracking"
        );
    }

    @Override
    public CommunicateReceiver getCommunicateReceiver() {
        return this;
    }

    @Communicate("command.normal")
    @CommunicateFilter(key = "name", value = "bug")
    public void doCommand(CommandCommunicateData commandCommunicateData) {
        if (commandCommunicateData.commandArgs.length != 1)
            return;
        CompletableFuture
            .runAsync(() -> analyzeBug(
                commandCommunicateData.messageManager,
                commandCommunicateData.targetData,
                commandCommunicateData.backendID,
                commandCommunicateData.commandArgs[0]
            ), getExecutorService())
            .exceptionally(t -> {
                commandCommunicateData.messageManager.sendMessage(
                    commandCommunicateData.backendID,
                    commandCommunicateData.targetData,
                    MessageChain.text("BugTracker：发生错误，错误报告如下：\n" + t.getMessage())
                );
                return null;
            });
    }

    private static final Pattern MC_BUG = Pattern.compile("(MC|MCPE|WEB)-\\d{1,6}");

    @Communicate("command.regex")
    @CommunicateFilter(key = "regex", value = "(MC|MCPE|WEB)-\\d{1,6}")
    public void doRegex(MessageCommunicateData messageCommunicateData) {
        String search = TextMessage.concatText(messageCommunicateData.messageChain);
        Matcher matcher = MC_BUG.matcher(search);
        getExecutorService().execute(() -> messageCommunicateData.messageManager.reactionMessage(
            messageCommunicateData.backendID,
            messageCommunicateData.messageChain,
            "waiting"
        ));
        while (matcher.find()) {
            CompletableFuture
                .runAsync(() -> analyzeBug(
                    messageCommunicateData.messageManager,
                    messageCommunicateData.targetData,
                    messageCommunicateData.backendID,
                    matcher.group()
                ), getExecutorService())
                .exceptionally(t -> {
                    messageCommunicateData.messageManager.sendMessage(
                        messageCommunicateData.backendID,
                        messageCommunicateData.targetData,
                        MessageChain.text("BugTracker：发生错误，错误报告如下：\n" + t.getMessage())
                    );
                    return null;
                });
        }
    }

    public static final String MOJIRA_API_URL = "https://bugs.mojang.com/rest/api/2/issue/";

    @SneakyThrows
    public void analyzeBug(MessageManager messageManager, TargetData target, String backend, String searchBug) {
        HttpGet get = new HttpGet(MOJIRA_API_URL + searchBug);
        JsonObject data = WebUtil.fetchDataInJson(get).getAsJsonObject();
        if (data.has("errorMessage"))
            throw new RuntimeException(data.get("errorMessage").getAsString());

        JsonObject fields = data.getAsJsonObject("fields");
        if (fields == null)
            throw new IOException("无法获取JSON文本，可能是该漏洞报告被删除或无权访问");

        String title = JsonUtil.getStringOrNull(data, "key") + ": " + JsonUtil.getStringOrNull(fields, "summary");
        String project = JsonUtil.getStringInPathOrNull(fields, "project.name");
        String created = JsonUtil.getStringOrNull(fields, "created");
        String status = JsonUtil.getStringInPathOrNull(fields, "status.name");
        String subStatus = JsonUtil.getStringInPathOrNull(fields, "customfield_10500.value");
        String resolution = JsonUtil.getStringInPathOrNull(fields, "resolution.name");
        String mojangPriority = JsonUtil.getStringInPathOrNull(fields, "customfield_12200.value");

        if (resolution == null)
            resolution = "Unresolved";
        if (resolution.equals("Duplicate")) {
            JsonArray issueLinks = fields.getAsJsonArray("issuelinks");
            for (JsonElement element : issueLinks) {
                JsonObject issue = element.getAsJsonObject();
                String type = JsonUtil.getStringInPathOrNull(issue, "type.name");
                String outwardIssue = JsonUtil.getStringInPathOrNull(issue, "outwardIssue.key");
                if (type != null && outwardIssue != null && type.equals("Duplicate")) {
                    resolution += "（与" + outwardIssue + "重复）";
                    break;
                }
            }
        }

        String versions = JsonUtil.getData(fields, "versions", JsonArray.class).map(versionsArray -> {
            if (versionsArray.size() == 1) {
                JsonObject versionRoot = versionsArray.get(0).getAsJsonObject();
                return JsonUtil.getStringOrNull(versionRoot, "name") + "("
                    + JsonUtil.getStringOrNull(versionRoot, "releaseDate") + ")";
            } else {
                JsonObject versionRoot1 = versionsArray.get(0).getAsJsonObject();
                JsonObject versionRoot2 = versionsArray.get(versionsArray.size() - 1).getAsJsonObject();
                return JsonUtil.getStringOrNull(versionRoot1, "name") + "("
                    + JsonUtil.getStringOrNull(versionRoot1, "releaseDate") + ") ~ " +
                    JsonUtil.getStringOrNull(versionRoot2, "name") + "("
                    + JsonUtil.getStringOrNull(versionRoot2, "releaseDate") + ")";
            }
        }).orElse(null);

        String finalResolution = resolution;
        String fixVersion = JsonUtil.getData(fields, "fixVersions", JsonArray.class).map(fixArray -> {
            if (!fixArray.isEmpty()) {
                JsonObject lastFix = fixArray.get(fixArray.size() - 1).getAsJsonObject();
                String ret = JsonUtil.getStringOrNull(lastFix, "name") + "("
                    + JsonUtil.getStringOrNull(lastFix, "releaseDate") + ")";
                if (!finalResolution.equals("Resolved") && !finalResolution.equals("Fixed"))
                    ret += "（尝试修复）";
                if (fixArray.size() > 1)
                    ret += "（仅显示最后一次修复）";
                return ret;
            } else
                return null;
        }).orElse(null);

        StringBuilder builder = new StringBuilder(title).append("\n");
        if (project != null)
            builder.append("项目: ").append(project).append("\n");
        if (created != null)
            builder.append("创建时间: ").append(created).append("\n");
        if (versions != null)
            builder.append("影响版本: ").append(versions).append("\n");
        builder.append("目前状态: ");
        if (subStatus != null)
            builder.append(subStatus);
        if (status != null)
            builder.append("[").append(status).append("]");
        builder.append("\n");
        builder.append("目前解决状态: ").append(resolution).append("\n");
        if (fixVersion != null)
            builder.append("修复版本: ").append(fixVersion).append("\n");
        if (mojangPriority != null)
            builder.append("Mojang优先级: ").append(mojangPriority).append("\n");
        builder.append("主条目URL: https://bugs.mojang.com/browse/").append(searchBug).append("\n");

        messageManager.sendMessage(backend, target, MessageChain.text(builder.toString()));

        Communication.
            <byte[]>communicateWithResult(
            "atribot-plugin-web-renderer",
            "webrenderer.render_page_element",
            Map.of(
                "page", "https://bugs.mojang.com/browse/" + searchBug,
                "element", ".aui-item.issue-main-column"
            )
        ).thenComposeAsync(image -> {
             if (image == null)
                 return CompletableFuture.completedFuture(null);
             return messageManager.getFileTransfer().sendFile(
                 backend,
                 new ByteArrayInputStream(image),
                 getExecutorService()
             );
         }, getExecutorService())
         .thenAccept(fileID -> {
             if (fileID == null)
                 return;
             messageManager.sendMessage(
                 backend,
                 target,
                 new MessageChain().next(new ImageMessage("", URI.create(fileID)))
             );
         })
         .exceptionallyAsync(e -> {
             String errorMessage = e.getMessage();
             errorMessage = errorMessage.length() > 200 ? errorMessage.substring(
                 0,
                 200
             ) + "..." : errorMessage;
             messageManager.sendMessage(
                 backend,
                 target,
                 MessageChain.text("BugTracker：渲染页面时出现错误，错误报告如下\n" + errorMessage)
             );
             return null;
         }, getExecutorService());
    }
}
