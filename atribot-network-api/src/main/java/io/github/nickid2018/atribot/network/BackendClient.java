package io.github.nickid2018.atribot.network;

import io.github.nickid2018.atribot.network.connection.*;
import io.github.nickid2018.atribot.network.listener.NetworkListener;
import io.github.nickid2018.atribot.network.packet.KeepAlivePacket;
import io.github.nickid2018.atribot.network.packet.Packet;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.InetAddress;
import java.util.function.Supplier;

@Getter
public class BackendClient {

    private final Connection connection;
    private final NetworkListener listener;
    private final PacketRegistry registry;

    private volatile boolean active;

    public BackendClient(NetworkListener listener) {
        this.listener = listener;
        registry = new PacketRegistry();
        registry.registerPacket(KeepAlivePacket.class, KeepAlivePacket::new, true, true);
        connection = new Connection(new DelegateListener(), registry, false);
    }

    public <T extends Packet> void addPacket(Class<T> packetClass, Supplier<T> supplier) {
        if (active)
            return;
        registry.registerPacket(packetClass, supplier, false, true);
    }

    public void connect(InetAddress addr, int port) {
        if (active)
            return;
        boolean epoll = Epoll.isAvailable();
        new Bootstrap()
                .group((epoll ? Connection.NETWORK_EPOLL_WORKER_GROUP : Connection.NETWORK_WORKER_GROUP).get())
                .handler(new ClientChannelInitializer(connection))
                .channel(epoll ? EpollSocketChannel.class : NioSocketChannel.class)
                .connect(addr, port)
                .syncUninterruptibly();
    }

    public void sendPacket(Packet packet) {
        connection.sendPacket(packet);
    }

    public void disconnect() {
        if (!active)
            return;
        connection.disconnect();
    }

    private class DelegateListener implements NetworkListener {

        @Override
        public void connectionOpened(Connection connection) {
            active = true;
            listener.connectionOpened(connection);
        }

        @Override
        public void receivePacket(Connection connection, Packet msg) {
            if (msg instanceof KeepAlivePacket)
                connection.sendPacket(msg);
            else
                listener.receivePacket(connection, msg);
        }

        @Override
        public void connectionClosed(Connection connection) {
            active = false;
            listener.connectionClosed(connection);
        }

        @Override
        public void exceptionCaught(Connection connection, Throwable cause) {
            listener.exceptionCaught(connection, cause);
        }

        @Override
        public void fatalError(Connection connection, Throwable cause) {
            listener.fatalError(connection, cause);
        }
    }

    @AllArgsConstructor
    private static class ClientChannelInitializer extends ChannelInitializer<Channel> {

        private final Connection connection;

        @Override
        protected void initChannel(Channel channel) {
            Connection.setupChannel(channel, connection);
        }
    }
}
