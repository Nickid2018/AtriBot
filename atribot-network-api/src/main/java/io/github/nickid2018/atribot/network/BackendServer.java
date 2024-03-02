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
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

@Slf4j
public class BackendServer {

    private final Supplier<NetworkListener> listenerSupplier;
    private final PacketRegistry registry;
    private boolean shouldEncrypt = false;

    private final Thread keepAliveThread;
    private final byte[] keepAliveThreadLock = new byte[0];

    private final Set<Connection> connections = Collections.synchronizedSet(new ObjectLinkedOpenHashSet<>());
    private volatile ChannelFuture future;


    public BackendServer(Supplier<NetworkListener> listenerSupplier) {
        this.listenerSupplier = listenerSupplier;

        registry = new PacketRegistry();
        registry.registerPacket(KeepAlivePacket.class, KeepAlivePacket::new, true, true);
        registry.registerPacket(EncryptionStartPacket.class, EncryptionStartPacket::new, true, false);
        registry.registerPacket(EncryptionProgressPacket.class, EncryptionProgressPacket::new, false, true);
        registry.registerPacket(ConnectionSuccessPacket.class, ConnectionSuccessPacket::new, true, false);

        keepAliveThread = new Thread(this::keepAlive, "Backend Server Keep Alive");
        keepAliveThread.setDaemon(true);
    }

    public <T extends Packet> void addPacket(Class<T> packetClass, Supplier<T> supplier, boolean serverSide, boolean clientSide) {
        registry.registerPacket(packetClass, supplier, serverSide, clientSide);
    }

    public void start(int port) {
        if (future != null)
            return;
        boolean epoll = Epoll.isAvailable();
        future = new ServerBootstrap()
                .channel(epoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .childHandler(new ServerChannelInitializer())
                .group((epoll ? Connection.SERVER_EPOLL_EVENT_GROUP : Connection.SERVER_EVENT_GROUP).get())
                .localAddress((InetAddress) null, port)
                .bind()
                .syncUninterruptibly();
        if (!keepAliveThread.isAlive())
            keepAliveThread.start();
    }

    public void stop() throws InterruptedException {
        if (future == null)
            return;
        synchronized (keepAliveThreadLock) {
            connections.forEach(Connection::disconnect);
            connections.clear();
            try {
                future.channel().close().sync();
            } finally {
                future = null;
                keepAliveThreadLock.notifyAll();
            }
        }
    }

    public BackendServer setShouldEncrypt(boolean shouldEncrypt) {
        if (future != null)
            return this;
        this.shouldEncrypt = shouldEncrypt;
        return this;
    }

    public void broadcastPacket(Packet packet) {
        connections.removeIf(Connection::isNotActive);
        connections.forEach(connection -> connection.sendPacket(packet));
    }

    @SneakyThrows
    private void keepAlive() {
        Thread.sleep(1000);
        while (future != null) {
            synchronized (keepAliveThreadLock) {
                broadcastPacket(KeepAlivePacket.createNow());
                try {
                    keepAliveThreadLock.wait(15000);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }
    }

    private class ServerChannelInitializer extends ChannelInitializer<Channel> {

        @Override
        protected void initChannel(Channel channel) {
            Connection connection = new Connection(new DelegateListener(listenerSupplier.get()), registry, true);
            Connection.setupChannel(channel, connection);
        }
    }

    @RequiredArgsConstructor
    private class DelegateListener implements NetworkListener {

        private final NetworkListener listener;

        private byte[] encryptionChallenge;
        private KeyPair keyPair;

        @Override
        public void connectionOpened(Connection connection) {
            if (shouldEncrypt) {
                keyPair = CipherHelper.generateKeyPair();
                encryptionChallenge = new byte[8];
                new SecureRandom().nextBytes(encryptionChallenge);
                connection.sendPacket(new EncryptionStartPacket(keyPair.getPublic().getEncoded(), encryptionChallenge));
            } else {
                connection.sendPacket(new ConnectionSuccessPacket());
                connections.add(connection);
                listener.connectionOpened(connection);
            }
        }

        @Override
        public void receivePacket(Connection connection, Packet msg) {
            if (msg instanceof KeepAlivePacket keepAlivePacket) {
                long sentTime = keepAlivePacket.getTime();
                long currentTime = System.currentTimeMillis();
                long ping = currentTime - sentTime;
                if (ping > 10000)
                    log.warn("Client {} has a high ping: {}ms", connection.getAddress(), ping);
            } else if (msg instanceof EncryptionProgressPacket encryptionProgressPacket) {
                byte[] decryptedChallenge = CipherHelper.decrypt(encryptionProgressPacket.getEncryptedChallenge(), keyPair.getPrivate());
                if (!Arrays.equals(decryptedChallenge, encryptionChallenge)) {
                    log.warn("Client {} has a wrong challenge", connection.getAddress());
                    connection.disconnect();
                    return;
                }
                byte[] decryptedKey = CipherHelper.decrypt(encryptionProgressPacket.getEncryptedSecretKey(), keyPair.getPrivate());
                encryptionChallenge = null;
                keyPair = null;
                SecretKey secretKey = CipherHelper.decodeSecretKey(decryptedKey);
                connection.getChannel().pipeline().addBefore("splitter", "decrypt", new CipherDecoder(secretKey));
                connection.getChannel().pipeline().addBefore("prepender", "encrypt", new CipherEncoder(secretKey));
                connection.sendPacket(new ConnectionSuccessPacket());
                connections.add(connection);
                listener.connectionOpened(connection);
            } else
                listener.receivePacket(connection, msg);
        }

        @Override
        public void connectionClosed(Connection connection) {
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
}
