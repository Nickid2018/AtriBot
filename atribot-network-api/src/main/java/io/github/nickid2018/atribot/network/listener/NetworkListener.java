package io.github.nickid2018.atribot.network.listener;

import io.github.nickid2018.atribot.network.connection.Connection;
import io.github.nickid2018.atribot.network.packet.Packet;

public interface NetworkListener {
    void connectionOpened(Connection connection);

    void receivePacket(Connection connection, Packet msg);

    void connectionClosed(Connection connection);

    void exceptionCaught(Connection connection, Throwable cause);

    void fatalError(Connection connection, Throwable cause);
}
