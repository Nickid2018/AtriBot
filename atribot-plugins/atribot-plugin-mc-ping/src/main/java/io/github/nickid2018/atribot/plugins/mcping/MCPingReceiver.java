package io.github.nickid2018.atribot.plugins.mcping;

import com.google.common.net.HostAndPort;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.message.CommandCommunicateData;
import io.github.nickid2018.atribot.core.message.MessageManager;
import io.github.nickid2018.atribot.network.message.ImageMessage;
import io.github.nickid2018.atribot.network.message.MessageChain;
import io.github.nickid2018.atribot.network.message.TargetData;
import io.github.nickid2018.atribot.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@AllArgsConstructor
public class MCPingReceiver implements CommunicateReceiver {

    private final MCPingPlugin plugin;
    private static final Set<String> COMMUNICATE_KEYS = Set.of("atribot.message.command");

    @Override
    public <T, D> CompletableFuture<T> communicate(String communicateKey, D data) {
        CommandCommunicateData commandData = (CommandCommunicateData) data;
        if (commandData.commandHead.equalsIgnoreCase("mc")) {
            TargetData targetData = commandData.targetData;
            MessageManager manager = commandData.messageManager;
            String backend = commandData.backendID;
            String[] args = commandData.commandArgs;
            plugin.getExecutorService().execute(() -> {
                if (args.length == 0 || args.length > 2) {
                    manager.sendMessage(backend, targetData, MessageChain.text("MCPing：无效的格式"));
                    return;
                }

                String addr = args[0];
                boolean je = true;
                if (args.length == 2) {
                    String serverType = args[1];
                    if (serverType.equalsIgnoreCase("be"))
                        je = false;
                    else if (!serverType.equalsIgnoreCase("je")) {
                        manager.sendMessage(backend, targetData, MessageChain.text("MCPing：无效的服务器类型"));
                        return;
                    }
                }

                HostAndPort hostAndPort = HostAndPort.fromString(addr).withDefaultPort(25565);
                InetSocketAddress address = new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort());
                if (address.getAddress().isLoopbackAddress()) {
                    manager.sendMessage(
                        backend,
                        targetData,
                        MessageChain.text("MCPing：由于安全设置，不能访问环回地址的MC服务器")
                    );
                    return;
                }

                if (je) {
                    try {
                        JsonObject json = new MCJEServerPing(address).fetchData();
                        StringBuilder sb = new StringBuilder();

                        sb.append("服务器：").append(addr).append("\n");
                        sb.append("延迟：").append(JsonUtil.getIntOrZero(json, "ping")).append("ms\n");
                        sb.append("版本：").append(JsonUtil.getStringInPathOrNull(json, "version.name"));
                        sb.append("（协议版本 ")
                          .append(JsonUtil.getIntInPathOrElse(json, "version.protocol", -1))
                          .append("）\n");

                        int onlinePlayers = JsonUtil.getIntInPathOrZero(json, "players.online");
                        sb.append("玩家数量：")
                          .append(onlinePlayers)
                          .append("/")
                          .append(JsonUtil.getIntInPathOrElse(json, "players.max", -1)).append("\n");

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

                        sb.append(JsonUtil.getStringInPathOrElse(
                            json, "description.text", "").replaceAll("§.", ""));
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
                    } catch (Exception e) {
                        manager.sendMessage(
                            backend,
                            targetData,
                            MessageChain.text("MCPing：获取信息时发生了异常：%s".formatted(e.getMessage()))
                        );
                    }
                } else {
                    try {
                        Map<String, String> mapData = new MCBEServerPing(address).fetchData();
                        StringBuilder sb = new StringBuilder();

                        sb.append("服务器：").append(addr).append("\n");
                        sb.append("延迟：").append(mapData.get("ping")).append("ms\n");
                        sb.append("类型：").append(mapData.get("edition")).append("\n");
                        sb.append("版本：")
                          .append(mapData.get("version"))
                          .append("（协议版本 ")
                          .append(mapData.get("protocol"))
                          .append("）\n");
                        sb.append("玩家数量：")
                          .append(mapData.get("players"))
                          .append("/")
                          .append(mapData.get("maxPlayers"))
                          .append("\n");
                        sb.append("游戏模式：").append(mapData.get("gamemode")).append("\n");
                        sb.append(mapData.get("motd1")).append("\n").append(mapData.get("motd2"));

                        manager.sendMessage(backend, targetData, MessageChain.text(sb.toString().trim()));
                    } catch (Exception e) {
                        manager.sendMessage(
                            backend,
                            targetData,
                            MessageChain.text("MCPing：获取信息时发生了异常：%s".formatted(e.getMessage()))
                        );
                    }
                }
            });
        }
        return null;
    }

    @Override
    public Set<String> availableCommunicateKeys() {
        return COMMUNICATE_KEYS;
    }
}
