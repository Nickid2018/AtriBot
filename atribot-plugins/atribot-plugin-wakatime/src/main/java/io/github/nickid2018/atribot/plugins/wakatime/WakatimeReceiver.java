package io.github.nickid2018.atribot.plugins.wakatime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.nickid2018.atribot.core.communicate.Communicate;
import io.github.nickid2018.atribot.core.communicate.CommunicateFilter;
import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.communicate.Communication;
import io.github.nickid2018.atribot.core.message.CommandCommunicateData;
import io.github.nickid2018.atribot.core.message.MessageManager;
import io.github.nickid2018.atribot.network.message.MessageChain;
import io.github.nickid2018.atribot.network.message.TargetData;
import io.github.nickid2018.atribot.util.FunctionUtil;
import io.github.nickid2018.atribot.util.JsonUtil;
import io.github.nickid2018.atribot.util.WebUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@Slf4j
@RequiredArgsConstructor
public class WakatimeReceiver implements CommunicateReceiver {

    private final WakatimePlugin plugin;

    public static final Pattern TIME_DURATION = Pattern.compile("\\d{8}-\\d{8}");

    @Communicate("oauth2.service.started")
    public void onOAuth2ServiceStarted() {
        plugin.registerOAuth2Service();
    }

    @Communicate("oauth2.service.stopped")
    public void onOAuth2ServiceStopped() {
        plugin.oauth2ServiceAvailable = false;
    }

    @Communicate("command.normal")
    @CommunicateFilter(key = "name", value = "wakatime")
    public void doCommand(CommandCommunicateData messageData) {
        String backendID = messageData.backendID;
        TargetData target = messageData.targetData;
        MessageManager manager = messageData.messageManager;

        if (!plugin.oauth2ServiceAvailable) {
            manager.sendMessage(backendID, target, MessageChain.text("Wakatime：OAuth 2.0服务不可用"));
            return;
        }

        if (messageData.commandArgs.length == 0) {
            printAllSummary(backendID, manager, target);
        } else if (messageData.commandArgs.length == 1) {
            switch (messageData.commandArgs[0]) {
                case "today" -> printSummary(
                    "https://wakatime.com/api/v1/users/current/summaries?range=Today",
                    backendID,
                    manager,
                    target
                );
                case "yesterday" -> printSummary(
                    "https://wakatime.com/api/v1/users/current/summaries?range=Yesterday",
                    backendID,
                    manager,
                    target
                );
                case "revoke" -> revoke(backendID, manager, target);
                default -> {
                    if (!TIME_DURATION.matcher(messageData.commandArgs[0]).matches()) {
                        plugin.getExecutorService().execute(() -> manager.sendMessage(
                            backendID,
                            target,
                            MessageChain.text("Wakatime：输入了无效的参数")
                        ));
                    } else {
                        String range = messageData.commandArgs[0];
                        String[] split = range.split("-");
                        printSummary(
                            "https://wakatime.com/api/v1/users/current/summaries?start=%s&end=%s".formatted(
                                split[0],
                                split[1]
                            ),
                            backendID,
                            manager,
                            target
                        );
                    }
                }
            }
        } else {
            plugin.getExecutorService().execute(() -> manager.sendMessage(
                backendID,
                target,
                MessageChain.text("Wakatime：输入了无效的参数")
            ));
        }
    }

    private void revoke(String backendID, MessageManager manager, TargetData target) {
        Communication.communicateWithResult(
            "atribot-plugin-oauth2-service",
            "oauth2.revoke",
            Map.of(
                "oauthName", "wakatime",
                "id", target.getTargetUser()
            )
        ).thenAcceptAsync(
            v -> manager.sendMessage(backendID, target, MessageChain.text("Wakatime：已撤销授权")),
            plugin.getExecutorService()
        ).exceptionallyAsync(t -> {
            log.error("Error in revoking", t);
            manager.sendMessage(backendID, target, MessageChain.text("Wakatime：撤销授权时出现错误：" + t.getMessage()));
            return null;
        }, plugin.getExecutorService());
    }

    public void printAllSummary(String backendID, MessageManager manager, TargetData targetData) {
        Communication
            .communicateWithResult(
                "atribot-plugin-oauth2-service",
                "oauth2.authenticate",
                Map.of(
                    "oauthName", "wakatime",
                    "backendID", backendID,
                    "target", targetData,
                    "manager", manager,
                    "scopes", List.of("read_logged_time")
                )
            )
            .thenAcceptAsync(FunctionUtil.tryOrElse(accessToken -> {
                HttpGet get = new HttpGet("https://wakatime.com/api/v1/users/current/stats/last_7_days");
                get.setHeader("Authorization", "Bearer " + accessToken);
                JsonObject data = WebUtil.fetchDataInJson(get).getAsJsonObject().getAsJsonObject("data");
                if (data == null)
                    throw new IllegalStateException("未获得正确数据，可能授权已过期，请使用 revoke 以清除授权状态");

                StringBuilder builder = new StringBuilder();

                builder.append(JsonUtil.getStringOrNull(data, "username")).append("\n");

                builder
                    .append("近七天编程时长: ")
                    .append(JsonUtil.getStringOrNull(data, "human_readable_total"))
                    .append("\n");
                builder
                    .append("平均编程时间: ")
                    .append(JsonUtil.getStringOrNull(data, "human_readable_daily_average"))
                    .append("\n");

                builder.append("编程语言占比:\n");
                JsonArray array = JsonUtil.getData(data, "languages", JsonArray.class).orElse(new JsonArray());
                StreamSupport
                    .stream(array.spliterator(), false)
                    .filter(JsonElement::isJsonObject)
                    .limit(5)
                    .map(JsonElement::getAsJsonObject)
                    .forEach(object -> {
                        String name = JsonUtil.getStringOrNull(object, "name");
                        String percent = "%.2f".formatted(object.get("percent").getAsFloat());
                        builder.append(name).append(": ").append(percent).append("%\n");
                    });

                manager.sendMessage(backendID, targetData, MessageChain.text(builder.toString().trim()));
            }, (t, e) -> whenFetchError(e, backendID, manager, targetData)), plugin.getExecutorService())
            .exceptionallyAsync(
                t -> whenAuthError(t, backendID, manager, targetData),
                plugin.getExecutorService()
            );
    }

    public void printSummary(String url, String backendID, MessageManager manager, TargetData targetData) {
        Communication
            .communicateWithResult(
                "atribot-plugin-oauth2-service",
                "oauth2.authenticate",
                Map.of(
                    "oauthName", "wakatime",
                    "backendID", backendID,
                    "target", targetData,
                    "manager", manager,
                    "scopes", List.of("read_logged_time")
                )
            )
            .thenAcceptAsync(FunctionUtil.tryOrElse(accessToken -> {
                HttpGet get = new HttpGet(url);
                get.addHeader("Authorization", "Bearer " + accessToken);

                JsonObject gotta = WebUtil.fetchDataInJson(get).getAsJsonObject();
                JsonObject cumulativeTotal = gotta.getAsJsonObject("cumulative_total");
                if (cumulativeTotal == null)
                    throw new IllegalStateException("未获得正确数据，可能授权已过期，请使用 revoke 以清除授权状态");

                double sec = JsonUtil.getData(cumulativeTotal, "seconds", JsonPrimitive.class)
                                     .filter(JsonPrimitive::isNumber)
                                     .map(JsonPrimitive::getAsDouble)
                                     .orElse(0.0);

                StringBuilder builder = new StringBuilder();

                builder.append("编程时长: ").append(JsonUtil.getStringOrNull(cumulativeTotal, "text")).append("\n");

                Map<String, Double> langTime = new HashMap<>();

                JsonArray array = JsonUtil
                    .getData(gotta, "data", JsonArray.class)
                    .orElse(new JsonArray());
                StreamSupport
                    .stream(array.spliterator(), false)
                    .filter(JsonElement::isJsonObject)
                    .map(JsonElement::getAsJsonObject)
                    .forEach(object -> {
                        JsonArray languages = JsonUtil
                            .getData(object, "languages", JsonArray.class)
                            .orElse(new JsonArray());
                        StreamSupport
                            .stream(languages.spliterator(), false)
                            .filter(JsonElement::isJsonObject)
                            .map(JsonElement::getAsJsonObject)
                            .forEach(language -> {
                                String lang = JsonUtil.getStringOrNull(language, "name");
                                double data = langTime.computeIfAbsent(lang, n -> 0.0);
                                langTime.put(lang, data +
                                    JsonUtil.getData(language, "total_seconds", JsonPrimitive.class)
                                            .filter(JsonPrimitive::isNumber)
                                            .map(JsonPrimitive::getAsDouble)
                                            .orElse(0.0));
                            });
                    });

                builder.append("编程语言占比:\n");

                langTime.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .limit(5)
                        .forEach(dat -> {
                            String name = dat.getKey();
                            String percent = "%.2f".formatted(dat.getValue() / sec * 100);
                            builder.append(name).append(": ").append(percent).append("%\n");
                        });

                manager.sendMessage(backendID, targetData, MessageChain.text(builder.toString().trim()));
            }, (t, e) -> whenFetchError(e, backendID, manager, targetData)), plugin.getExecutorService())
            .exceptionallyAsync(
                t -> whenAuthError(t, backendID, manager, targetData),
                plugin.getExecutorService()
            );
    }

    private void whenFetchError(Throwable t, String backendID, MessageManager manager, TargetData targetData) {
        log.error("Error in fetching data", t);
        manager.sendMessage(
            backendID,
            targetData,
            MessageChain.text("Wakatime：获取数据时出现错误：" + t.getMessage())
        );
    }

    private <T> T whenAuthError(Throwable t, String backendID, MessageManager manager, TargetData targetData) {
        if (t instanceof TimeoutException) {
            manager.sendMessage(backendID, targetData, MessageChain.text("Wakatime：授权超时"));
        } else if (t instanceof ExecutionException) {
            log.error("Error in fetching data", t);
            manager.sendMessage(backendID, targetData, MessageChain.text("Wakatime：授权错误：" + t.getCause().getMessage()));
        } else {
            log.error("Error in fetching data", t);
            manager.sendMessage(backendID, targetData, MessageChain.text("Wakatime：授权错误：" + t.getMessage()));
        }
        return null;
    }
}
