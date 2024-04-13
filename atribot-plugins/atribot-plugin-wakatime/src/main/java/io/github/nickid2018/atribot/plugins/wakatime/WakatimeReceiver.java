package io.github.nickid2018.atribot.plugins.wakatime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.StreamSupport;

@Slf4j
@RequiredArgsConstructor
public class WakatimeReceiver implements CommunicateReceiver {

    private final WakatimePlugin plugin;
    private static final Set<String> keys = Set.of("atribot.message.command");

    @Override
    public <T, D> CompletableFuture<T> communicate(String communicateKey, D data) {
        CommandCommunicateData messageData = (CommandCommunicateData) data;
        if (!messageData.commandHead.equals("wakatime"))
            return null;

        String backendID = messageData.backendID;
        TargetData target = messageData.targetData;
        MessageManager manager = messageData.messageManager;

        if (messageData.commandArgs.length == 0) {

        } else {
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
            }
        }

        return null;
    }

    public void printSummary(String url, String backendID, MessageManager manager, TargetData targetData) {
        Communication.<Map<?, ?>, String>communicateWithResult("OAuth2Plugin[dev]", "oauth2.authenticate", Map.of(
            "oauthName", "wakatime",
            "backendID", backendID,
            "target", targetData,
            "manager", manager,
            "scopes", List.of("read_logged_time")
        )).thenAcceptAsync(FunctionUtil.noExceptionOrElse(accessToken -> {
            HttpGet get = new HttpGet(url);
            get.addHeader("Authorization", "Bearer " + accessToken);

            JsonObject gotta = WebUtil.fetchDataInJson(get).getAsJsonObject();
            JsonObject cumulative_total = gotta.getAsJsonObject("cumulative_total");
            if (cumulative_total == null)
                throw new IllegalStateException("未获得正确数据，可能授权已过期");

            double sec = JsonUtil.getData(cumulative_total, "seconds", JsonPrimitive.class)
                                 .filter(JsonPrimitive::isNumber)
                                 .map(JsonPrimitive::getAsDouble)
                                 .orElse(0.0);

            StringBuilder builder = new StringBuilder();

            builder.append("编程时长: ").append(JsonUtil.getStringOrNull(cumulative_total, "text")).append("\n");

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
                    .sorted((e1, e2) -> (int) (e2.getValue() - e1.getValue()))
                    .limit(5)
                    .forEach(dat -> {
                        String name = dat.getKey();
                        String percent = "%.2f".formatted(dat.getValue() / sec * 100);
                        builder.append(name).append(": ").append(percent).append("%\n");
                    });

            manager.sendMessage(backendID, targetData, MessageChain.text(builder.toString()));
        }, (t, e) -> {
            log.error("Error in fetching data", e);
            manager.sendMessage(backendID, targetData, MessageChain.text("获取数据时出现错误：" + e.getMessage()));
        }), plugin.getExecutorService());
    }

    @Override
    public Set<String> availableCommunicateKeys() {
        return keys;
    }
}
