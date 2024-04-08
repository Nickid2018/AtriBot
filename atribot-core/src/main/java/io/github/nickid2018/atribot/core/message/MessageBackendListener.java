package io.github.nickid2018.atribot.core.message;

import io.github.nickid2018.atribot.network.connection.Connection;
import io.github.nickid2018.atribot.network.listener.NetworkListener;
import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.backend.BackendBasicInformationPacket;
import io.github.nickid2018.atribot.network.packet.backend.MessagePacket;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageBackendListener implements NetworkListener {

    private MessageManager manager;

    public MessageBackendListener(MessageManager manager) {
        this.manager = manager;
    }

    @Override
    public void connectionOpened(Connection connection) {
    }

    @Override
    public void receivePacket(Connection connection, Packet msg) {
        switch (msg) {
            case BackendBasicInformationPacket packet -> {
                log.info("Backend connected: {} {}", packet.getIdentifier(), packet.getVersion());
                log.debug("Backend information: {} => {}", packet.getIdentifier(), packet.getExternalInformation());
            }
            case MessagePacket packet -> manager.handleMessage(packet.getTargetData(), packet.getMessageChain());
            default -> throw new IllegalStateException("Unexpected value: " + msg);
        }
    }

    @Override
    public void connectionClosed(Connection connection) {

    }

    @Override
    public void exceptionCaught(Connection connection, Throwable cause) {
        log.error("Error in connection", cause);
    }

    @Override
    public void fatalError(Connection connection, Throwable cause) {
        log.error("Fatal error in connection", cause);
    }
}
