package io.github.nickid2018.atribot.network;

import io.github.nickid2018.atribot.network.connection.CipherDecoder;
import io.github.nickid2018.atribot.network.connection.CipherEncoder;
import io.github.nickid2018.atribot.network.connection.Connection;
import io.github.nickid2018.atribot.network.connection.PacketRegistry;
import io.github.nickid2018.atribot.network.listener.NetworkListener;
import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.common.ConnectionSuccessPacket;
import io.github.nickid2018.atribot.network.packet.common.EncryptionProgressPacket;
import io.github.nickid2018.atribot.network.packet.common.EncryptionStartPacket;
import io.github.nickid2018.atribot.network.packet.common.KeepAlivePacket;
import io.github.nickid2018.atribot.util.CipherHelper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.net.InetAddress;
import java.security.PublicKey;
import java.util.function.Supplier;

@Getter
@Slf4j
public class BackendClient implements PacketRegister {

    private final Connection connection;
    private final NetworkListener listener;
    private final PacketRegistry registry;

    private volatile boolean active;
    private volatile boolean encrypted;

    public BackendClient(NetworkListener listener) {
        this.listener = listener;
        registry = new PacketRegistry();
        PacketRegister.registerBasicPackets(this);
        connection = new Connection(new DelegateListener(), registry, false);
    }

    public <T extends Packet> void addPacket(Class<T> packetClass, Supplier<T> supplier, boolean serverSide, boolean clientSide) {
        registry.registerPacket(packetClass, supplier, serverSide, clientSide);
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
        }

        @Override
        public void receivePacket(Connection connection, Packet msg) {
            if (msg instanceof KeepAlivePacket)
                connection.sendPacket(msg);
            else if (msg instanceof EncryptionStartPacket encryptionStartPacket) {
                if (encrypted) {
                    listener.fatalError(connection, new IllegalStateException("Received encryption start packet after encryption started"));
                    connection.disconnect();
                    return;
                }
                SecretKey key = CipherHelper.generateSecretKey();
                byte[] challenge = encryptionStartPacket.getChallenge();
                byte[] serverPublicKey = encryptionStartPacket.getPublicKey();
                PublicKey publicKey = CipherHelper.decodePublicKey(serverPublicKey);
                byte[] encryptedChallenge = CipherHelper.encrypt(challenge, publicKey);
                byte[] encryptedKey = CipherHelper.encrypt(key.getEncoded(), publicKey);
                connection.sendPacket(new EncryptionProgressPacket(encryptedKey, encryptedChallenge), v -> {
                    connection.getChannel().pipeline().addBefore("splitter", "decrypt", new CipherDecoder(key));
                    connection.getChannel().pipeline().addBefore("prepender", "encrypt", new CipherEncoder(key));
                    encrypted = true;
                });
            } else if (msg instanceof ConnectionSuccessPacket) {
                active = true;
                listener.connectionOpened(connection);
            } else
                listener.receivePacket(connection, msg);
        }

        @Override
        public void connectionClosed(Connection connection) {
            if (active) {
                active = false;
                listener.connectionClosed(connection);
            }
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
