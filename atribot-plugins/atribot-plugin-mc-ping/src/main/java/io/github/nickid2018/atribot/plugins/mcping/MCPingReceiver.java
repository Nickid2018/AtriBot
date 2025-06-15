package io.github.nickid2018.atribot.plugins.mcping;

import com.google.common.net.HostAndPort;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.atribot.core.communicate.Communicate;
import io.github.nickid2018.atribot.core.communicate.CommunicateFilter;
import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.communicate.Communication;
import io.github.nickid2018.atribot.core.message.CommandCommunicateData;
import io.github.nickid2018.atribot.core.message.MessageManager;
import io.github.nickid2018.atribot.network.message.ImageMessage;
import io.github.nickid2018.atribot.network.message.MessageChain;
import io.github.nickid2018.atribot.network.message.TargetData;
import io.github.nickid2018.atribot.util.FunctionUtil;
import io.github.nickid2018.atribot.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class MCPingReceiver implements CommunicateReceiver {

    private final MCPingPlugin plugin;

    @Communicate("command.normal")
    @CommunicateFilter(key = "name", value = "mc")
    public void communicateMC(CommandCommunicateData commandData) {
        communicateBase(commandData, false);
    }

    @Communicate("command.normal")
    @CommunicateFilter(key = "name", value = "mct")
    public void communicateMCT(CommandCommunicateData commandData) {
        communicateBase(commandData, true);
    }

    public void communicateBase(CommandCommunicateData commandData, boolean isText) {
        TargetData targetData = commandData.targetData;
        MessageManager manager = commandData.messageManager;
        String backend = commandData.backendID;
        String[] args = commandData.commandArgs;
        plugin.getExecutorService().execute(() -> {
            if (args.length != 1) {
                manager.sendMessage(backend, targetData, MessageChain.text("MCPing：无效的格式"));
                return;
            }

            String addr = args[0];

            HostAndPort hostAndPort = HostAndPort.fromString(addr).withDefaultPort(25565);

            try {
                try {
                    JsonObject json = new MCJEServerPing(hostAndPort).fetchData();
                    if (isText) {
                        sendJEPlain(addr, json, manager, backend, targetData);
                    } else {
                        Map<String, Object> data = new HashMap<>();
                        data.put("html", prepareRenderingTemplate(json));
                        data.put("element", "#base");
                        Communication.<byte[]>communicateWithResult(
                            "atribot-plugin-web-renderer",
                            "webrenderer.render_html_element",
                            data
                        ).thenComposeAsync(
                            result -> manager
                                .getFileTransfer()
                                .sendFile(backend, new ByteArrayInputStream(result), plugin.getExecutorService()),
                            plugin.getExecutorService()
                        ).thenAccept(fileID -> {
                            if (fileID == null)
                                return;
                            manager.sendMessage(
                                backend,
                                targetData,
                                new MessageChain().next(new ImageMessage("", URI.create(fileID)))
                            );
                        }).exceptionallyAsync(
                            FunctionUtil.sneakyThrowsFunc(e -> {
                                sendJEPlain(addr, json, manager, backend, targetData);
                                log.error("Failed to render HTML element for MCPing", e);
                                return null;
                            }), plugin.getExecutorService()
                        ).join();
                    }
                } catch (IOException e) {
                    sendBE(addr, HostAndPort.fromString(addr).withDefaultPort(19132), manager, backend, targetData);
                }
            } catch (Exception e) {
                manager.sendMessage(
                    backend,
                    targetData,
                    MessageChain.text("MCPing：获取信息时发生了异常：%s".formatted(e.getMessage()))
                );
            }
        });
    }

    private String escapeHtml(String str) {
        return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace(
            "'",
            "&#39;"
        ).replace(" ", "&nbsp;").replace("\n", "<br/>");
    }

    private String parseLiteral(String literal) {
        StringBuilder result = new StringBuilder();
        int depth = 0;
        while (literal.indexOf('§') >= 0) {
            int index = literal.indexOf('§');
            result.append(escapeHtml(literal.substring(0, index)));
            char c = literal.charAt(index + 1);
            literal = literal.substring(index + 2);
            if (c == 'r') {
                result.repeat("</span>", depth);
                depth = 0;
            } else {
                switch (c) {
                    case '0' -> result.append("<span class=\"black\">");
                    case '1' -> result.append("<span class=\"dark_blue\">");
                    case '2' -> result.append("<span class=\"dark_green\">");
                    case '3' -> result.append("<span class=\"dark_aqua\">");
                    case '4' -> result.append("<span class=\"dark_red\">");
                    case '5' -> result.append("<span class=\"dark_purple\">");
                    case '6' -> result.append("<span class=\"gold\">");
                    case '7' -> result.append("<span class=\"gray\">");
                    case '8' -> result.append("<span class=\"dark_gray\">");
                    case '9' -> result.append("<span class=\"blue\">");
                    case 'a' -> result.append("<span class=\"green\">");
                    case 'b' -> result.append("<span class=\"aqua\">");
                    case 'c' -> result.append("<span class=\"red\">");
                    case 'd' -> result.append("<span class=\"light_purple\">");
                    case 'e' -> result.append("<span class=\"yellow\">");
                    case 'f' -> result.append("<span class=\"white\">");
                    case 'l' -> result.append("<span class=\"bold\">");
                    case 'm' -> result.append("<span class=\"strike\">");
                    case 'n' -> result.append("<span class=\"underline\">");
                    case 'o' -> result.append("<span class=\"italic\">");
                    case 'k' -> result.append("<span class=\"obfuscated\">");
                }
                depth++;
            }
        }
        result.append(escapeHtml(literal));
        result.repeat("</span>", depth);
        return result.toString();
    }

    private String parseTextComponent(JsonObject component) {
        List<String> classList = new ArrayList<>();
        StringBuilder styleBuilder = new StringBuilder();
        StringBuilder result = new StringBuilder();
        if (component.has("color")) {
            String color = component.get("color").getAsString();
            if (color.startsWith("#"))
                styleBuilder.append("color: ").append(color).append("; ");
            else
                classList.add(color);
        }
        if (component.has("shadow_color")) {
            JsonElement shadowColor = component.get("shadow_color");
            String colorStr;
            if (shadowColor.isJsonPrimitive())
                colorStr = String.format("#%06X", shadowColor.getAsInt() & 0xFFFFFF);
            else {
                int[] rgba = shadowColor.getAsJsonArray().asList().stream().mapToInt(JsonElement::getAsInt).toArray();
                colorStr = String.format("#%02X%02X%02X", rgba[0], rgba[1], rgba[2]);
            }
            styleBuilder.append("--shadow-color: ").append(colorStr).append(";");
        }
        if (component.has("bold") && component.get("bold").getAsBoolean())
            classList.add("bold");
        if (component.has("italic") && component.get("italic").getAsBoolean())
            classList.add("italic");
        if (component.has("strikethrough") && component.get("strikethrough").getAsBoolean())
            classList.add("strike");
        if (component.has("underlined") && component.get("underlined").getAsBoolean())
            classList.add("underlined");
        if (component.has("obfuscated") && component.get("obfuscated").getAsBoolean())
            classList.add("obfuscated");
        result
            .append("<span class=\"")
            .append(String.join(" ", classList))
            .append("\" style=\"")
            .append(styleBuilder)
            .append("\">")
            .append(component.has("text") ? escapeHtml(component.get("text").getAsString()) : "[PARSING FAILED]");
        if (component.has("extra")) {
            JsonArray extra = component.getAsJsonArray("extra");
            for (JsonElement element : extra) {
                if (element.isJsonPrimitive()) {
                    result.append(parseLiteral(element.getAsString()));
                } else if (element.isJsonObject()) {
                    result.append(parseTextComponent(element.getAsJsonObject()));
                }
            }
        }
        result.append("</span>");
        return result.toString();
    }

    private String prepareOnlinePlayers(JsonObject json) {
        JsonArray players = JsonUtil.getDataInPath(json, "players.sample", JsonArray.class).orElse(null);
        if (players == null || players.isEmpty())
            return "";
        List<String> playerData = new ArrayList<>();
        for (JsonElement element : players) {
            JsonObject player = element.getAsJsonObject();
            String name = JsonUtil.getStringOrNull(player, "name");
            String uuid = JsonUtil.getStringOrElse(player, "id", "").replace("-", "");
            if (name != null) {
                playerData.add(STR."""
                                   <div style="display: flex; gap: 4px; align-items: center;">
                                     <img src="https://crafatar.com/avatars/\{uuid}?size=16" width="16px" height="16px" />
                                     <span>\{name}</span>
                                   </div>
                                   """);
            }
        }
        return STR."""
                   <div>
                     <div style="text-align: center; margin-bottom: 2px; background-color: grey;">玩家列表</div>
                     <div style="display: grid; grid-gap: 10px; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));">
                       \{String.join("", playerData)}
                     </div>
                   </div>
                   """;
    }

    private String prepareOtherInfos(JsonObject json) {
        String desc = json
            .entrySet()
            .stream()
            .filter(entry -> !Constants.KNOWN_PROPERTIES.contains(entry.getKey()))
            .map(entry -> {
                String key = entry.getKey();
                String data = Constants.KNOWN_EXTENSIONS.containsKey(key)
                              ? Constants.KNOWN_EXTENSIONS.get(key).apply(entry.getValue())
                              : "";
                return data.isEmpty()
                       ? STR."<span class=\"italic\" style=\"color: grey;\">\{key}</span>"
                       : STR."<div>\{data}<span class=\"italic\" style=\"color: grey;\">(\{key})</span></div>";
            })
            .collect(Collectors.joining());
        return desc.isEmpty()
               ? ""
               : STR."""
                     <div>
                       <div style="text-align: center; margin-bottom: 2px; background-color: grey;">其他数据</div>
                       <div style="display: flex; gap: 2px; flex-direction: column;">\{desc}</div>
                     </div>
                     """;
    }

    private String prepareRenderingTemplate(JsonObject json) {
        String favicon = JsonUtil.getStringInPathOrElse(json, "favicon", Constants.UNKNOWN_SERVER_FAVICON);
        int onlinePlayers = JsonUtil.getIntInPathOrZero(json, "players.online");
        int maxPlayers = JsonUtil.getIntInPathOrZero(json, "players.max");
        String version = JsonUtil.getStringInPathOrNull(json, "version.name");
        int protocol = JsonUtil.getIntInPathOrElse(json, "version.protocol", -1);
        int ping = JsonUtil.getIntOrZero(json, "ping");
        int index = ping < 150 ? 5 : ping < 300 ? 4 : ping < 600 ? 3 : ping < 1000 ? 2 : 1;
        JsonObject descriptionJson = JsonUtil.getDataInPath(json, "description", JsonObject.class).orElse(null);
        String motd = descriptionJson == null ? parseLiteral(JsonUtil.getStringOrElse(
            json,
            "description",
            ""
        )) : parseTextComponent(descriptionJson);
        return STR."""
               <!DOCTYPE html>
               <head>
                 <style>
                   @font-face {
                     font-family: Minecraft;
                     src: url("https://zh.minecraft.wiki/images/Minecraft.woff?0f52c") format("woff");
                   }
                   html { --shadow-color: #3f3f3f; }
                   .black { color: #000000; --shadow-color: #000000; }
                   .dark_blue { color: #0000aa; --shadow-color: #00002a; }
                   .dark_green { color: #00aa00; --shadow-color: #002a00; }
                   .dark_aqua { color: #00aaaa; --shadow-color: #002a2a; }
                   .dark_red { color: #aa0000; --shadow-color: #2a0000; }
                   .dark_purple { color: #aa00aa; --shadow-color: #2a002a; }
                   .gold { color: #ffaa00; --shadow-color: #2a2a00; }
                   .gray { color: #aaaaaa; --shadow-color: #2a2a2a; }
                   .dark_gray { color: #555555; --shadow-color: #151515; }
                   .blue { color: #5555ff; --shadow-color: #15153f; }
                   .green { color: #55ff55; --shadow-color: #153f15; }
                   .aqua { color: #55ffff; --shadow-color: #153f3f; }
                   .red { color: #ff5555; --shadow-color: #3f1515; }
                   .light_purple { color: #ff55ff; --shadow-color: #3f153f; }
                   .yellow { color: #ffff55; --shadow-color: #3f3f15; }
                   .white { color: #ffffff; --shadow-color: #3f3f3f; }
                   .underline { text-decoration: underline; }
                   .bold { font-weight: bold; }
                   .strike { text-decoration: line-through; }
                   .italic { font-style: italic; }
                   .obfuscated { background-color: #ffaa00; }
                   span:last-child::after { padding-left: 1px; content: ' '; }
                 </style>
               </head>
               <html style="font-family: Minecraft, Unifont; background-color: #8e8e8e; color: white; image-rendering: pixelated; text-shadow: .125em .125em 0 var(--shadow-color)">
                 <div id="base" style="width: fit-content; height: fit-content; min-width: 200px; display: flex; flex-direction: column; padding: 5px; gap: 5px;">
                   <div style="display: flex; flex-direction: row; align-items: flex-start; gap: 10px;">
                     <img src="\{favicon}" />
                     <div style="flex-grow: 1; display: flex; flex-direction: column; align-items: flex-start; gap: 2px;">
                       <span>Java 版服务器</span>
                       <div class="gray" style="line-height: 22px; height: 42px; overflow-y: hidden;">\{motd}</div>
                     </div>
                     <div style="display: flex; flex-direction: column; align-items: flex-end; gap: 2px;">
                       <div style="display: flex; gap: 5px; align-items:center;">
                         <span>\{ping}ms</span>
                         <img src="\{Constants.PING_SPRITE[index - 1]}" width="20px" height="16px" />
                       </div>
                       <div>\{onlinePlayers}/\{maxPlayers}</div>
                       <div>\{version} (\{protocol})</div>
                     </div>
                   </div>
                   \{prepareOnlinePlayers(json)}
                   \{prepareOtherInfos(json)}
                 </div>
               </html>
               """;
    }

    private void sendJEPlain(String addr, JsonObject json, MessageManager manager, String backend, TargetData targetData) throws IOException {
        StringBuilder sb = new StringBuilder();

        sb.append("服务器（Java版）：").append(addr).append("\n");
        sb.append("延迟：").append(JsonUtil.getIntOrZero(json, "ping")).append("ms\n");
        sb.append("版本：").append(JsonUtil.getStringInPathOrNull(json, "version.name"));
        sb.append("（协议版本 ").append(JsonUtil.getIntInPathOrElse(json, "version.protocol", -1)).append("）\n");

        int onlinePlayers = JsonUtil.getIntInPathOrZero(json, "players.online");
        sb.append("玩家数量：").append(onlinePlayers).append("/").append(JsonUtil.getIntInPathOrElse(
            json,
            "players.max",
            -1
        )).append("\n");

        JsonUtil.getDataInPath(json, "players.sample", JsonArray.class).ifPresent(array -> {
            sb.append("目前玩家：");
            if (array.size() < onlinePlayers)
                sb.append("（仅显示一部分）");
            sb.append("\n");

            List<String> players = new ArrayList<>();
            for (JsonElement element : array)
                players.add(JsonUtil.getStringOrNull(element.getAsJsonObject(), "name"));
            sb.append(String.join("，", players)).append("\n");
        });

        sb.append(JsonUtil.getStringInPathOrElse(json, "description", "").replaceAll("§.", ""));
        MessageChain chain = MessageChain.text(sb.toString().trim());

        Optional<String> favicon = JsonUtil.getString(json, "favicon");
        if (favicon.isPresent()) {
            String base64 = favicon.get().split(",", 2)[1];
            byte[] bytes = Base64.getDecoder().decode(base64);
            CompletableFuture<String> future = manager.getFileTransfer().sendFile(
                backend,
                new ByteArrayInputStream(bytes),
                plugin.getExecutorService()
            );
            future.thenAccept(url -> chain.next(new ImageMessage(
                RandomStringUtils.random(16, true, true),
                URI.create(url)
            )));
        }

        manager.sendMessage(backend, targetData, chain);
    }

    private void sendBE(String addr, HostAndPort address, MessageManager manager, String backend, TargetData targetData) throws IOException {
        Map<String, String> mapData = new MCBEServerPing(address).fetchData();
        StringBuilder sb = new StringBuilder();

        sb.append("服务器（基岩版）：").append(addr).append("\n");
        sb.append("延迟：").append(mapData.get("ping")).append("ms\n");
        sb.append("类型：").append(mapData.get("edition")).append("\n");
        sb.append("版本：").append(mapData.get("version")).append("（协议版本 ").append(mapData.get("protocol")).append(
            "）\n");
        sb
            .append("玩家数量：")
            .append(mapData.get("players"))
            .append("/")
            .append(mapData.get("maxPlayers"))
            .append("\n");
        sb.append("游戏模式：").append(mapData.get("gamemode")).append("\n");
        sb.append(mapData.get("motd1")).append("\n").append(mapData.get("motd2"));

        manager.sendMessage(backend, targetData, MessageChain.text(sb.toString().trim()));
    }
}
