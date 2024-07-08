package io.github.nickid2018.atribot.core.message;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.github.nickid2018.atribot.network.connection.Connection;
import io.github.nickid2018.atribot.network.listener.NetworkListener;
import io.github.nickid2018.atribot.network.message.TransactionQueue;
import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.backend.BackendBasicInformationPacket;
import io.github.nickid2018.atribot.network.packet.backend.MessagePacket;
import io.github.nickid2018.atribot.network.packet.backend.MessageSentPacket;
import io.github.nickid2018.atribot.network.packet.backend.QueuedMessageRequestPacket;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MessageBackendListener implements NetworkListener {

    private final MessageManager manager;
    private final BiMap<Connection, String> connectionMap = HashBiMap.create();
    private final Map<String , TransactionQueue> transactionQueueMap = new HashMap<>();

    public MessageBackendListener(MessageManager manager) {
        this.manager = manager;
    }

    @Override
    public void connectionOpened(Connection connection) {
    }

    @Override
    public void receivePacket(Connection connection, Packet msg) {
        String id = connectionMap.get(connection);
        if (id != null) {
            TransactionQueue queue = transactionQueueMap.get(id);
            if (queue != null && queue.resolveTransaction(msg)) {
                log.debug("Transaction resolved: {}", msg);
                return;
            }
        }
        switch (msg) {
            case BackendBasicInformationPacket packet -> {
                manager.backendConnected(
                    packet.getIdentifier(),
                    packet.getVersion(),
                    packet.getExternalInformation()
                );
                connectionMap.put(connection, packet.getIdentifier());
                transactionQueueMap.put(packet.getIdentifier(), new TransactionQueue(() -> connection));
            }
            case MessagePacket packet -> manager.handleMessage(
                connectionMap.get(connection),
                packet.getTargetData(),
                packet.getMessageChain()
            );
            case MessageSentPacket packet -> manager.messageSent(packet.getUniqueID());
            case QueuedMessageRequestPacket ignored -> manager.clearQueue(id);
            default -> throw new IllegalStateException("Unexpected value: " + msg);
        }
    }

    @Override
    public void connectionClosed(Connection connection) {
        String id = connectionMap.remove(connection);
        if (id != null) {
            transactionQueueMap.remove(id).close();
            manager.backendDisconnected(id);
        }
    }

    @Override
    public void exceptionCaught(Connection connection, Throwable cause) {
        log.error("Error in connection", cause);
    }

    @Override
    public void fatalError(Connection connection, Throwable cause) {
        log.error("Fatal error in connection", cause);
    }

    public void sendPacket(String backendID, Packet packet) {
        Connection connection = connectionMap.inverse().get(backendID);
        if (connection != null)
            connection.sendPacket(packet);
    }

    public TransactionQueue getTransactionQueue(String backendID) {
        return transactionQueueMap.get(backendID);
    }
}
