package io.github.nickid2018.atribot.network;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class BackendClient implements PacketRegister {

    @Getter
    private final NetworkListener listener;
    @Getter
    private final PacketRegistry registry;

    @Getter
    private volatile Connection connection;
    @Getter
    private volatile boolean active;
    @Getter
    private volatile boolean encrypted;
    @Getter
    private volatile boolean autoReconnect = false;
    private volatile ScheduledFuture<?> reconnectFuture;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            1, new ThreadFactoryBuilder().setNameFormat("BackendClient-Scheduler").setDaemon(true).build());

    public BackendClient(NetworkListener listener) {
        this.listener = listener;
        registry = new PacketRegistry();
        PacketRegister.registerBasicPackets(this);
    }

    public <T extends Packet> void addPacket(Class<T> packetClass, Supplier<T> supplier, boolean serverSide, boolean clientSide) {
        registry.registerPacket(packetClass, supplier, serverSide, clientSide);
    }

    public void connect(InetAddress addr, int port) {
        if (connection != null || active) {
            log.warn("Connection already opened");
            return;
        }
        try {
            boolean epoll = Epoll.isAvailable();
            new Bootstrap()
                    .group((epoll ? Connection.NETWORK_EPOLL_WORKER_GROUP : Connection.NETWORK_WORKER_GROUP).get())
                    .handler(new ClientChannelInitializer())
                    .channel(epoll ? EpollSocketChannel.class : NioSocketChannel.class)
                    .connect(addr, port)
                    .syncUninterruptibly();
        } catch (Exception e) {
            log.error("Error connecting to backend server", e);
            connection = null;
            if (autoReconnect && reconnectFuture == null) {
                reconnectFuture = scheduler.scheduleAtFixedRate(() -> {
                    log.info("Trying to connect to backend server: {}:{}", addr, port);
                    connect(addr, port);
                }, 30, 40, TimeUnit.SECONDS);
            }
        }
    }

    public void sendPacket(Packet packet) {
        if (connection != null && active)
            connection.sendPacket(packet);
    }

    public void disconnect() {
        if (!active)
            return;
        autoReconnect = false;
        if (reconnectFuture != null)
            reconnectFuture.cancel(false);
        if (connection != null)
            connection.disconnect();
    }

    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
        if (!autoReconnect && reconnectFuture != null)
            reconnectFuture.cancel(false);
    }

    private class DelegateListener implements NetworkListener {

        @Override
        public void connectionOpened(Connection connection) {
            if (reconnectFuture != null) {
                reconnectFuture.cancel(false);
                reconnectFuture = null;
            }
        }

        @Override
        public void receivePacket(Connection connection, Packet msg) {
            switch (msg) {
                case KeepAlivePacket ignored -> connection.sendPacket(msg);
                case EncryptionStartPacket encryptionStartPacket -> {
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
                }
                case ConnectionSuccessPacket ignored -> {
                    active = true;
                    listener.connectionOpened(connection);
                }
                case null, default -> listener.receivePacket(connection, msg);
            }
        }

        @Override
        public void connectionClosed(Connection connection) {
            if (active) {
                active = false;
                encrypted = false;
                BackendClient.this.connection = null;
                listener.connectionClosed(connection);
                if (autoReconnect) {
                    log.info("Connection closed, start reconnecting scheduler");
                    reconnectFuture = scheduler.scheduleAtFixedRate(() -> {
                        InetSocketAddress address = (InetSocketAddress) connection.getAddress();
                        log.info("Reconnecting to backend server: {}:{}", address.getAddress(), address.getPort());
                        connect(address.getAddress(), address.getPort());
                    }, 30, 40, TimeUnit.SECONDS);
                }
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

    private class ClientChannelInitializer extends ChannelInitializer<Channel> {
        @Override
        protected void initChannel(Channel channel) {
            Connection.setupChannel(channel, connection = new Connection(new DelegateListener(), registry, false));
        }
    }
}
