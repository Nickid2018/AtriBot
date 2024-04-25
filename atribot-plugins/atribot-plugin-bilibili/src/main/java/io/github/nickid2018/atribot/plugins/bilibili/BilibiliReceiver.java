package io.github.nickid2018.atribot.plugins.bilibili;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.message.MessageCommunicateData;
import io.github.nickid2018.atribot.network.message.ImageMessage;
import io.github.nickid2018.atribot.network.message.MessageChain;
import io.github.nickid2018.atribot.network.message.TextMessage;
import io.github.nickid2018.atribot.util.FunctionUtil;
import io.github.nickid2018.atribot.util.JsonUtil;
import io.github.nickid2018.atribot.util.WebUtil;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.hc.client5.http.classic.methods.HttpGet;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
public class BilibiliReceiver implements CommunicateReceiver {

    private static final Set<String> KEY = Set.of("atribot.message.normal");

    public static final Pattern B_AV_VIDEO_PATTERN = Pattern.compile("[aA][vV][1-9]\\d{0,9}");
    public static final Pattern B_BV_VIDEO_PATTERN = Pattern.compile(
        "[bB][vV][fZodR9XQDSUm21yCkr6zBqiveYah8bt4xsWpHnJE7jL5VG3guMTKNPAwcF]{10}");
    public static final Pattern B_SHORT_LINK_PATTERN = Pattern.compile("https://b23\\.tv/[0-9a-zA-Z]+");

    private final BilibiliPlugin plugin;

    @Override
    public <T, D> CompletableFuture<T> communicate(String communicateKey, D data) throws Exception {
        MessageCommunicateData messageData = (MessageCommunicateData) data;
        String message = TextMessage.concatText(messageData.messageChain);
        plugin.getExecutorService().execute(FunctionUtil.tryOrElse(() -> {
            Matcher matcher = B_SHORT_LINK_PATTERN.matcher(message);
            if (matcher.find()) {
                String shortLink = matcher.group();
                String redirected = WebUtil.getRedirected(new HttpGet(shortLink));
                tryGetInfo(redirected, messageData);
            } else
                tryGetInfo(message, messageData);
        }, e -> messageData.messageManager.sendMessage(
            messageData.backendID,
            messageData.targetData,
            MessageChain.text("Bilibili：获取信息失败，错误报告如下\n" + e.getMessage())
        )));
        return null;
    }

    @SneakyThrows
    private void tryGetInfo(String content, MessageCommunicateData messageCommunicateData) {
        Matcher avMatcher = B_AV_VIDEO_PATTERN.matcher(content);
        if (avMatcher.find()) {
            String av = avMatcher.group().substring(2);
            JsonObject data = WebUtil
                .fetchDataInJson(new HttpGet("https://api.bilibili.com/x/web-interface/view?aid=" + av))
                .getAsJsonObject();
            getVideoInfo(av, data, messageCommunicateData);
            return;
        }

        Matcher bvMatcher = B_BV_VIDEO_PATTERN.matcher(content);
        if (bvMatcher.find()) {
            String bv = bvMatcher.group();
            JsonObject data = WebUtil
                .fetchDataInJson(new HttpGet("https://api.bilibili.com/x/web-interface/view?bvid=" + bv))
                .getAsJsonObject();
            getVideoInfo(bv, data, messageCommunicateData);
            return;
        }
    }

    @SneakyThrows
    private void getVideoInfo(String name, JsonObject videoData, MessageCommunicateData messageCommunicateData) {
        int code = JsonUtil.getIntOrZero(videoData, "code");
        if (code != 0) {
            messageCommunicateData.messageManager.sendMessage(
                messageCommunicateData.backendID,
                messageCommunicateData.targetData,
                MessageChain.text("Bilibili：获取信息失败\n错误代码：%d\n附加信息：%s".formatted(
                    code,
                    JsonUtil.getStringOrNull(videoData, "message")
                ))
            );
            return;
        }

        JsonObject data = videoData.getAsJsonObject("data");
        StringBuilder builder = new StringBuilder();
        String bvid = JsonUtil.getStringOrNull(data, "bvid");
        builder.append(bvid).append("\n");
        if (!name.equals(bvid))
            builder.append("（视频编号已从 ").append(name).append(" 自动转换）").append("\n");

        builder.append("视频标题：").append(JsonUtil.getStringOrNull(data, "title")).append("\n");
        builder
            .append("视频类型：")
            .append(JsonUtil.getIntOrZero(data, "copyright") == 1 ? "自制" : "转载")
            .append("\n");

        Optional<JsonArray> staffArray = JsonUtil.getData(data, "staff", JsonArray.class);
        if (staffArray.isPresent()) {
            builder.append("制作团队：");
            List<String> staffList = new ArrayList<>();
            for (JsonElement element : staffArray.get()) {
                JsonObject staff = element.getAsJsonObject();
                String staffName = JsonUtil.getStringOrNull(staff, "name");
                String title = JsonUtil.getStringOrNull(staff, "title");
                staffList.add(staffName + "（" + title + "）");
            }
            builder.append(String.join("、", staffList));
        } else
            builder.append("作者：").append(JsonUtil.getStringInPathOrNull(data, "owner.name"));
        builder.append("\n");

        JsonObject stats = data.getAsJsonObject("stat");
        builder.append("播放量：").append(JsonUtil.getIntOrZero(stats, "view")).append(" | ");
        builder.append("弹幕数：").append(JsonUtil.getIntOrZero(stats, "danmaku")).append(" | ");
        builder.append("评论数：").append(JsonUtil.getIntOrZero(stats, "reply")).append("\n");
        builder.append("点赞数：").append(JsonUtil.getIntOrZero(stats, "like")).append(" | ");
        builder.append("硬币数：").append(JsonUtil.getIntOrZero(stats, "coin")).append(" | ");
        builder.append("收藏数：").append(JsonUtil.getIntOrZero(stats, "favorite")).append(" | ");
        builder.append("分享数：").append(JsonUtil.getIntOrZero(stats, "share")).append("\n");

        long publishTime = JsonUtil.getIntOrZero(data, "pubdate") * 1000L;
        Date date = new Date(publishTime);
        builder.append("发布时间：").append(String.format("%tY/%tm/%td %tT", date, date, date, date)).append("\n");

        builder.append("视频总长度：").append(formatTime(JsonUtil.getIntOrZero(data, "duration")));
        int videos = JsonUtil.getIntOrZero(data, "videos");
        if (videos > 1)
            builder.append("（共 ").append(videos).append(" 个视频）");
        builder.append("\n");

        builder.append("https://www.bilibili.com/video/").append(bvid).append("\n");

        BufferedReader reader = new BufferedReader(new StringReader(JsonUtil.getStringOrNull(data, "desc")));
        String line;
        while ((line = reader.readLine()) != null && builder.length() <= 400) {
            line = line.trim();
            if (!line.isEmpty())
                builder.append(line).append("\n");
        }
        if (line != null)
            builder.append("（简介过长截断）");

        String imageURL = JsonUtil.getStringOrNull(data, "pic");
        if (imageURL != null)
            messageCommunicateData.messageManager.sendMessage(
                messageCommunicateData.backendID,
                messageCommunicateData.targetData,
                MessageChain.text(builder.toString()).next(new ImageMessage("", URI.create(imageURL)))
            );
        else
            messageCommunicateData.messageManager.sendMessage(
                messageCommunicateData.backendID,
                messageCommunicateData.targetData,
                MessageChain.text(builder.toString())
            );
    }

    private static String formatTime(int time) {
        if (time < 60)
            return time + "s";
        if (time < 3600)
            return time / 60 + "m" + time % 60 + "s";
        return time / 3600 + "h" + (time % 3600) / 60 + "m" + time % 60 + "s";
    }

    @Override
    public Set<String> availableCommunicateKeys() {
        return KEY;
    }
}
