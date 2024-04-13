package io.github.nickid2018.atribot.backend.console;

import io.github.nickid2018.atribot.network.connection.Connection;
import io.github.nickid2018.atribot.network.listener.NetworkListener;
import io.github.nickid2018.atribot.network.message.*;
import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.backend.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ConsoleBackendListener implements NetworkListener {

    private TransactionQueue transactionQueue;

    public void setTransactionQueue(TransactionQueue transactionQueue) {
        this.transactionQueue = transactionQueue;
        transactionQueue.registerTransactionConsumer(
                ImageResolveStartPacket.class,
                packet -> transactionQueue
                        .getConnection()
                        .sendPacket(new StopTransactionPacket(packet.getTransactionId()))
        );
    }

    @Override
    public void connectionOpened(Connection connection) {
        connection.sendPacket(new BackendBasicInformationPacket(
                "console",
                "1.0",
                Map.of(
                        "forwardMessageSupport", "false",
                        "selfId", "console",
                        "prefixCommand", ""
                )
        ));
        connection.sendPacket(QueuedMessageRequestPacket.INSTANCE);
        log.info("Connection opened");
    }

    @Override
    public void receivePacket(Connection connection, Packet msg) {
        if (transactionQueue.resolveTransaction(msg))
            return;
        if (msg instanceof SendMessagePacket packet) {
            MessageChain chain = packet.getMessageChain();
            String message = chain.flatten().getMessages().stream().map(m -> switch (m) {
                case TextMessage textMessage -> textMessage.getText();
                case ImageMessage imageMessage -> "[Image " + imageMessage.getImgKey() + "]";
                case UnsupportedMessage ignored -> "[Unsupported]";
                default -> "";
            }).collect(Collectors.joining());
            log.info("Received message: {}", message);
            connection.sendPacket(new MessageSentPacket(packet.getUniqueID()));
        }
    }

    @Override
    public void connectionClosed(Connection connection) {
        log.info("Connection closed");
    }

    @Override
    public void exceptionCaught(Connection connection, Throwable cause) {
        log.error("Exception in connection: ", cause);
    }

    @Override
    public void fatalError(Connection connection, Throwable cause) {
        log.error("Fatal error in connection: ", cause);
    }
}
