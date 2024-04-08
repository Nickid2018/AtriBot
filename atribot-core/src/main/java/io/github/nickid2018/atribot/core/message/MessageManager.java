package io.github.nickid2018.atribot.core.message;

import com.j256.ormlite.dao.Dao;
import io.github.nickid2018.atribot.network.message.MessageChain;
import io.github.nickid2018.atribot.network.message.TargetData;
import io.github.nickid2018.atribot.util.Configuration;
import io.github.nickid2018.atribot.core.database.DatabaseManager;
import io.github.nickid2018.atribot.core.message.persist.IdBackendMapping;
import io.github.nickid2018.atribot.network.BackendServer;
import io.github.nickid2018.atribot.network.PacketRegister;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class MessageManager {

    private final BackendServer server;
    private final DatabaseManager messageDatabase;
    private final Dao<IdBackendMapping, String> idBackendMappingDao;

    public MessageManager() throws Exception {
        server = new BackendServer(() -> new MessageBackendListener(this));
        PacketRegister.registerBackendPackets(server);

        String queueMessage = Configuration.getStringOrElse("database.queue_message", "database/queue_message.db");
        messageDatabase = new DatabaseManager(queueMessage);
        idBackendMappingDao = messageDatabase.getTable(IdBackendMapping.class);
        log.info("Message Queue Database linked to {}", queueMessage);
    }

    public void start() {
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

    public void handleMessage(TargetData target, MessageChain messageChain) {

    }
}
