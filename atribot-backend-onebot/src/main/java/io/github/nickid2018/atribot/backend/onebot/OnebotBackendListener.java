package io.github.nickid2018.atribot.backend.onebot;

import cn.evole.onebot.client.OneBotClient;
import cn.evole.onebot.client.annotations.SubscribeEvent;
import cn.evole.onebot.client.core.Bot;
import cn.evole.onebot.client.interfaces.Listener;
import cn.evole.onebot.sdk.entity.ArrayMsg;
import cn.evole.onebot.sdk.enums.MsgType;
import cn.evole.onebot.sdk.event.message.GroupMessageEvent;
import cn.evole.onebot.sdk.event.message.PrivateMessageEvent;
import cn.evole.onebot.sdk.util.MsgUtils;
import io.github.nickid2018.atribot.network.BackendClient;
import io.github.nickid2018.atribot.network.connection.Connection;
import io.github.nickid2018.atribot.network.listener.NetworkListener;
import io.github.nickid2018.atribot.network.message.*;
import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.backend.*;
import io.github.nickid2018.atribot.util.Configuration;
import io.github.nickid2018.atribot.util.FunctionUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
public class OnebotBackendListener implements NetworkListener, Listener {

    private final OneBotClient oneBotClient;
    private final Bot bot;

    @Setter
    private BackendClient client;
    private TransactionQueue transactionQueue;

    public OnebotBackendListener(OneBotClient oneBotClient) {
        this.oneBotClient = oneBotClient;
        this.bot = oneBotClient.getBot();
        oneBotClient.registerEvents(this);
    }

    public void setTransactionQueue(TransactionQueue transactionQueue) {
        this.transactionQueue = transactionQueue;
        transactionQueue.registerTransactionConsumer(
            ImageResolveStartPacket.class,
            packet -> transactionQueue
                .getConnection().get()
                .sendPacket(new StopTransactionPacket(packet.getTransactionId()))
        );
    }

    @Override
    public void connectionOpened(Connection connection) {
        connection.sendPacket(new BackendBasicInformationPacket(
            Configuration.getStringOrElse("onebot.name", "onebot"),
            "1.0",
            Map.of(
                "forwardMessageSupport", "true",
                "prefixCommand", "/"
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
                    bot.sendGroupMsg(groupIDLong, messages, true);
                } else {
                    String userID = target.getTargetUser();
                    long userIDLong = Long.parseLong(userID.substring(userID.lastIndexOf(".") + 1));
                    bot.sendPrivateMsg(userIDLong, messages, true);
                }
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
        if (event.getAnonymous() != null)
            return;
        String groupId = Configuration.getStringOrElse("onebot.name", "onebot") + ".group." + event.getGroupId();
        String userId = Configuration.getStringOrElse("onebot.name", "onebot") + ".user." + event.getUserId();
        TargetData target = new TargetData(groupId, userId);
        List<ArrayMsg> messages = event.getArrayMsg();
        MessageChain messageChain = parseMessage(messages);
        client.sendPacket(new MessagePacket(target, messageChain));
    }

    @SubscribeEvent
    public void listenUserMessageEvent(PrivateMessageEvent event) {
        String userId = Configuration.getStringOrElse("onebot.name", "onebot") + ".user." + event.getUserId();
        TargetData target = new TargetData(null, userId);
        List<ArrayMsg> messages = event.getArrayMsg();
        MessageChain messageChain = parseMessage(messages);
        client.sendPacket(new MessagePacket(target, messageChain));
    }

    private MessageChain parseMessage(List<ArrayMsg> messages) {
        MessageChain messageChain = new MessageChain();
        messages.forEach(FunctionUtil.noException(arrayMsg -> {
            MsgType type = arrayMsg.getType();
            Map<String, String> data = arrayMsg.getData();
            switch (type) {
                case text -> messageChain.next(new TextMessage(data.get("text"), false));
                case image -> messageChain.next(new ImageMessage(
                    RandomStringUtils.random(32),
                    URI.create(data.get("url"))
                ));
                case at -> {
                    String at = data.get("qq");
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

    private String parseMsg(MessageChain messageChain) {
        MsgUtils builder = MsgUtils.builder();
        messageChain.getMessages().forEach(FunctionUtil.noException(message -> {
            switch (message) {
                case TextMessage textMessage -> builder.text(textMessage.getText());
                case ImageMessage imageMessage -> builder.img(imageMessage.getResolved().toURL().toString());
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
