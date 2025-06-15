package io.github.nickid2018.atribot.backend.onebot;

import cn.evole.onebot.client.OneBotClient;
import cn.evole.onebot.client.annotations.SubscribeEvent;
import cn.evole.onebot.client.core.BotConfig;
import cn.evole.onebot.client.interfaces.Listener;
import cn.evole.onebot.sdk.event.message.GroupMessageEvent;
import cn.evole.onebot.sdk.event.message.PrivateMessageEvent;
import cn.evole.onebot.sdk.util.BotUtils;
import cn.evole.onebot.sdk.util.MsgUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.nickid2018.atribot.network.BackendClient;
import io.github.nickid2018.atribot.network.connection.Connection;
import io.github.nickid2018.atribot.network.file.TransferFileResolver;
import io.github.nickid2018.atribot.network.listener.NetworkListener;
import io.github.nickid2018.atribot.network.message.*;
import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.backend.*;
import io.github.nickid2018.atribot.util.Configuration;
import io.github.nickid2018.atribot.util.FunctionUtil;
import io.github.nickid2018.atribot.util.JsonUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.net.URI;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class OnebotBackendListener implements NetworkListener, Listener {

    private final OneBotClient oneBotClient;

    @Setter
    private BackendClient client;
    private TransactionQueue transactionQueue;
    private final ExecutorService fileTransferExecutor = Executors.newCachedThreadPool(
        new ThreadFactoryBuilder().setNameFormat("File Transfer #%d").setDaemon(true).build()
    );
    private final Cache<String, String> imageCache = CacheBuilder
        .newBuilder()
        .expireAfterWrite(60, TimeUnit.MINUTES)
        .concurrencyLevel(8)
        .maximumSize(1000)
        .softValues()
        .build();

    public OnebotBackendListener(OneBotClient oneBotClient) {
        this.oneBotClient = oneBotClient;
        oneBotClient.registerEvents(this);

        BotConfig config = oneBotClient.getConfig();
        log.info("Bot ID: {}", config.getBotId());
        log.info("Bot URL: {}", config.getUrl());
        if (config.getToken() != null)
            log.info("Bot Access Token: {}", "*".repeat(config.getToken().length()));
    }

    public void setTransactionQueue(TransactionQueue transactionQueue) {
        this.transactionQueue = transactionQueue;
        transactionQueue.registerTransactionConsumer(
            ImageResolveStartPacket.class,
            packet -> {
                Set<String> key = packet.getImageMessageKeys();
                Set<ImageMessage> resolved = key.stream().map(imgKey -> {
                    String file = imageCache.getIfPresent(imgKey);
                    if (file == null) {
                        log.warn("Can't find image {} from cache, outdated?", imgKey);
                        return null;
                    }
                    String imageCacheDir = Configuration.getStringOrElse("onebot.image_cache_dir", "./image_cache");
                    File[] files = new File(imageCacheDir).listFiles();
                    if (files == null)
                        return null;
                    File findFile = Stream
                        .of(files)
                        .map(f -> new File(f, "Ori"))
                        .map(File::listFiles)
                        .filter(Objects::nonNull)
                        .flatMap(Stream::of)
                        .filter(f -> f.getName().equalsIgnoreCase(file))
                        .max(Comparator.comparingLong(File::lastModified))
                        .orElse(null);
                    if (findFile == null) {
                        log.warn("Can't find image {} because no image found with name '{}'", imgKey, file);
                        return null;
                    }
                    String transfer = TransferFileResolver.transferFile(
                        Configuration.getStringOrElse("outgoing.transfer_type", "file"),
                        Configuration.getStringOrElse("outgoing.transfer_arg", "./transfer=>./transfer"),
                        findFile,
                        fileTransferExecutor
                    ).join();
                    return new ImageMessage(imgKey, URI.create(transfer));
                }).filter(Objects::nonNull).collect(Collectors.toSet());
                ImageResolveResultPacket resolvedPacket = new ImageResolveResultPacket(resolved);
                resolvedPacket.setTransactionId(packet.getTransactionId());
                client.sendPacket(resolvedPacket);
            }
        );
    }

    @Override
    public void connectionOpened(Connection connection) {
        String name = Configuration.getStringOrElse("onebot.name", "onebot");
        long botId = Configuration.getLongOrThrow(
            "onebot.bot_id",
            () -> new IllegalArgumentException("Please set onebot.bot_id in configuration file!")
        );
        connection.sendPacket(new BackendBasicInformationPacket(
            name,
            "1.0",
            Map.of(
                "forwardMessageSupport", "true",
                "prefixCommand", Configuration.getStringOrElse("onebot.command_prefix", "~"),
                "selfId", name + ".user." + botId,
                "ingoingTransferType", Configuration.getStringOrElse("ingoing.transfer_type", "file"),
                "ingoingTransferArg", Configuration.getStringOrElse("ingoing.transfer_arg", ".=>.")
            )
        ));
        connection.sendPacket(QueuedMessageRequestPacket.INSTANCE);
        log.info("Connection opened");
    }

    @Override
    public void receivePacket(Connection connection, Packet msg) {
        if (transactionQueue.resolveTransaction(msg))
            return;
        switch (msg) {
            case SendMessagePacket message -> {
                TargetData target = message.getTargetData();
                MessageChain messageChain = message.getMessageChain();
                String messages = parseMsg(messageChain);

                if (target.isGroupMessage()) {
                    String groupID = target.getTargetGroup();
                    long groupIDLong = Long.parseLong(groupID.substring(groupID.lastIndexOf(".") + 1));
                    oneBotClient.getBot().sendGroupMsg(groupIDLong, BotUtils.rawToJson(messages), true);
                } else {
                    String userID = target.getTargetUser();
                    long userIDLong = Long.parseLong(userID.substring(userID.lastIndexOf(".") + 1));
                    oneBotClient.getBot().sendPrivateMsg(userIDLong, BotUtils.rawToJson(messages), true);
                }

                connection.sendPacket(new MessageSentPacket(message.getUniqueID()));
            }
            case SendReactionPacket reaction -> {
                String msgID = reaction.getMessageID();
                String reactionType = reaction.getReaction();
                String reactionID = parseReaction(reactionType);
                JsonObject obj = new JsonObject();
                obj.addProperty("message_id", msgID);
                obj.addProperty("emoji_id", reactionID);
                oneBotClient.getActionFactory().action(
                    oneBotClient.getBot().getChannel(),
                    ReactionActionPath.INSTANCE,
                    obj
                );
            }
            default -> throw new IllegalStateException("Unexpected value: " + msg);
        }
    }

    @Override
    public void connectionClosed(Connection connection) {
        log.info("Connection closed");
    }

    @Override
    public void exceptionCaught(Connection connection, Throwable cause) {
        log.error("Exception occurred in connection", cause);
    }

    @Override
    public void fatalError(Connection connection, Throwable cause) {
        log.error("Fatal error occurred in connection", cause);
    }

    @SubscribeEvent
    public void listenGroupMessageEvent(GroupMessageEvent event) {
        try {
            if (event.getAnonymous() != null)
                return;
            String groupId = Configuration.getStringOrElse("onebot.name", "onebot") + ".group." + event.getGroupId();
            String userId = Configuration.getStringOrElse("onebot.name", "onebot") + ".user." + event.getUserId();
            TargetData target = new TargetData(groupId, userId);
            String msgID = String.valueOf(event.getMessageId());
            MessageChain messageChain = parseMessage(event.getMessage()).next(new MsgIDMessage(msgID));
            client.sendPacket(new MessagePacket(target, messageChain));
        } catch (Exception e) {
            log.error("Error occurred in group message event {}", event.getMessage(), e);
        }
    }

    @SubscribeEvent
    public void listenUserMessageEvent(PrivateMessageEvent event) {
        try {
            String userId = Configuration.getStringOrElse("onebot.name", "onebot") + ".user." + event.getUserId();
            TargetData target = new TargetData(null, userId);
            MessageChain messageChain = parseMessage(event.getMessage());
            client.sendPacket(new MessagePacket(target, messageChain));
        } catch (Exception e) {
            log.error("Error occurred in user message event {}", event.getMessage(), e);
        }
    }

    private MessageChain parseMessage(String messages) {
        MessageChain messageChain = new MessageChain();
        JsonArray array = JsonParser.parseString(messages).getAsJsonArray();
        array.forEach(FunctionUtil.sneakyThrowsConsumer(arrayMsg -> {
            JsonObject obj = arrayMsg.getAsJsonObject();
            String typeStr = JsonUtil.getStringOrElse(obj, "type", "text");
            switch (typeStr) {
                case "text" -> messageChain.next(new TextMessage(
                    JsonUtil.getStringInPathOrElse(obj, "data.text", ""),
                    false
                ));
                case "image" -> {
                    String imgKey = RandomStringUtils.random(32, true, true);
                    messageChain.next(new ImageMessage(imgKey, null));
                    String file = JsonUtil.getStringInPathOrElse(obj, "data.file", "");
                    imageCache.put(imgKey, file);
                }
                case "at" -> {
                    String at = JsonUtil.getStringInPathOrElse(obj, "data.qq", "all");
                    if (!at.equals("all"))
                        messageChain.next(new AtMessage(new TargetData(
                            null,
                            Configuration.getStringOrElse("onebot.name", "onebot") + ".user." + at
                        )));
                }
                default -> {
                }
            }
        }));
        return messageChain;
    }

    private String parseReaction(String reactionType) {
        return switch (reactionType) {
            case "waiting" -> "212";
            default -> "10060";
        };
    }

    private String parseMsg(MessageChain messageChain) {
        MsgUtils builder = MsgUtils.builder();
        messageChain.getMessages().forEach(FunctionUtil.sneakyThrowsConsumer(message -> {
            switch (message) {
                case TextMessage textMessage -> builder.text(textMessage.getText());
                case ImageMessage imageMessage -> builder.img(
                    imageMessage
                        .getResolved()
                        .toURL()
                        .toString()
                        .replace("file:", "file://")
                );
                case AtMessage atMessage -> {
                    TargetData target = atMessage.getTargetData();
                    if (!target.isUserSpecified())
                        return;
                    String userID = target.getTargetUser();
                    builder.at(Long.parseLong(userID.substring(userID.lastIndexOf(".") + 1)));
                }
                default -> {
                }
            }
        }));
        return builder.build();
    }
}
