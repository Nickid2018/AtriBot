package io.github.nickid2018.atribot.core.message;

import com.j256.ormlite.dao.Dao;
import io.github.nickid2018.atribot.core.communicate.Communication;
import io.github.nickid2018.atribot.core.database.DatabaseManager;
import io.github.nickid2018.atribot.core.message.persist.MessageQueueEntry;
import io.github.nickid2018.atribot.network.BackendServer;
import io.github.nickid2018.atribot.network.PacketRegister;
import io.github.nickid2018.atribot.network.message.MessageChain;
import io.github.nickid2018.atribot.network.message.TargetData;
import io.github.nickid2018.atribot.network.message.TextMessage;
import io.github.nickid2018.atribot.network.packet.backend.SendMessagePacket;
import io.github.nickid2018.atribot.util.Configuration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Slf4j
public class MessageManager {

    private final MessageBackendListener listener;
    private final BackendServer server;
    private final DatabaseManager messageDatabase;
    private final Map<String, Map<String, String>> backendInformation = new HashMap<>();
    private final Dao<MessageQueueEntry, String> messageQueueDao;

    public MessageManager() throws Exception {
        listener = new MessageBackendListener(this);
        server = new BackendServer(() -> listener);
        PacketRegister.registerBackendPackets(server);

        String queueMessage = Configuration.getStringOrElse("database.queue_message", "database/queue_message.db");
        messageDatabase = new DatabaseManager(queueMessage);
        messageQueueDao = messageDatabase.getTable(MessageQueueEntry.class);
        log.info("Message Queue Database linked to {}", queueMessage);
    }

    public void start() {
        boolean shouldEncrypt = Configuration.getBooleanOrElse("network.encrypted", false);
        if (shouldEncrypt)
            log.info("The backend server is using encryption");
        server.setShouldEncrypt(shouldEncrypt);
        int port = Configuration.getIntOrElse("network.backend_server_port", 11451);
        server.start(port);
        log.info("Backend Server started on port {}", port);
    }

    public void stop() {
        try {
            server.stop();
        } catch (InterruptedException e) {
            log.error("Error stopping Backend Server", e);
        }
        try {
            messageDatabase.close();
        } catch (Exception e) {
            log.error("Error closing Message Queue Database", e);
        }
        log.info("Backend Server stopped");
    }

    public void backendConnected(String backendID, String version, Map<String, String> externalInformation) {
        backendInformation.put(backendID, externalInformation);
        log.info("Backend connected: {} {}", backendID, version);
        log.debug("Backend information: {} => {}", backendID, externalInformation);
    }

    public void backendDisconnected(String backendID) {
        backendInformation.remove(backendID);
        log.info("Backend disconnected: {}", backendID);
    }

    public void handleMessage(String backendID, TargetData target, MessageChain messageChain) {
        String textPlain = TextMessage.concatText(messageChain);

        Map<String, String> backendInfo = backendInformation.get(backendID);
        String prefixCommand = backendInfo.getOrDefault("prefixCommand", "/");
        if (textPlain.startsWith(prefixCommand)) {
            List<String> commandLine = getCommandLine(textPlain, prefixCommand);
            String[] args = commandLine.subList(1, commandLine.size()).toArray(String[]::new);
            String command = commandLine.getFirst();
            Communication.communicate(
                "atribot.message.command",
                new CommandCommunicateData(backendID, messageChain, target, this, command, args)
            );
        } else
            Communication.communicate(
                "atribot.message.normal",
                new MessageCommunicateData(backendID, messageChain, target, this)
            );
    }

    public void sendMessage(String backendID, TargetData target, MessageChain messageChain) {
        String uniqueID = RandomStringUtils.random(32);
        long time = System.currentTimeMillis();
        SendMessagePacket packet = new SendMessagePacket(uniqueID, target, messageChain);
        MessageQueueEntry entry = new MessageQueueEntry(uniqueID, backendID, target, messageChain, time);
        try {
            messageQueueDao.create(entry);
        } catch (SQLException e) {
            log.error("Error saving message to queue", e);
        }
        listener.sendPacket(backendID, packet);
    }

    public void messageSent(String uniqueID) {
        try {
            messageQueueDao.deleteById(uniqueID);
        } catch (Exception e) {
            log.error("Error deleting message from queue", e);
        }
    }

    public void clearQueue(String backendID) {
        try {
            messageQueueDao.queryForEq("backend_id", backendID).forEach(entry -> {
                SendMessagePacket packet = new SendMessagePacket(entry.id, entry.sendTarget, entry.messageChain);
                listener.sendPacket(backendID, packet);
            });
        } catch (SQLException e) {
            log.error("Error clearing message queue", e);
        }
    }

    public void clearQueueForce() {
        try {
            messageQueueDao.delete(messageQueueDao.queryForAll());
        } catch (SQLException e) {
            log.error("Error clearing message queue", e);
        }
    }

    private static List<String> getCommandLine(String textPlain, String prefixCommand) {
        List<String> commandLine = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        boolean escape = false;

        char[] text = textPlain.substring(prefixCommand.length()).toCharArray();
        for (char c : text) {
            if (escape) {
                current.append(c);
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                inQuote = !inQuote;
                continue;
            }
            if (c == ' ' && !inQuote) {
                commandLine.add(current.toString());
                current = new StringBuilder();
                continue;
            }
            current.append(c);
        }
        commandLine.add(current.toString());
        commandLine.removeIf(String::isEmpty);
        return commandLine;
    }
}
