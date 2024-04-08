package io.github.nickid2018.atribot.network.connection;

import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.nickid2018.atribot.network.listener.NetworkListener;
import io.github.nickid2018.atribot.network.packet.Packet;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.CodecException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.TimeoutException;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.net.SocketAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

@Getter
public class Connection extends SimpleChannelInboundHandler<Packet> {

    public static final Supplier<NioEventLoopGroup> SERVER_EVENT_GROUP = Suppliers.memoize(() -> new NioEventLoopGroup(
        0,
        new ThreadFactoryBuilder()
            .setNameFormat("Netty Server IO #%d")
            .setDaemon(true)
            .build()
    ));

    public static final Supplier<EpollEventLoopGroup> SERVER_EPOLL_EVENT_GROUP = Suppliers.memoize(() -> new EpollEventLoopGroup(
        0,
        new ThreadFactoryBuilder()
            .setNameFormat("Epoll Server IO #%d")
            .setDaemon(true)
            .build()
    ));

    public static final Supplier<NioEventLoopGroup> NETWORK_WORKER_GROUP = Suppliers.memoize(() -> new NioEventLoopGroup(
        0,
        new ThreadFactoryBuilder()
            .setNameFormat("Netty Client IO #%d")
            .setDaemon(true)
            .build()
    ));

    public static final Supplier<EpollEventLoopGroup> NETWORK_EPOLL_WORKER_GROUP = Suppliers.memoize(() -> new EpollEventLoopGroup(
        0,
        new ThreadFactoryBuilder()
            .setNameFormat("Epoll Client IO #%d")
            .setDaemon(true)
            .build()
    ));

    public static final Logger NETWORK_LOGGER = LoggerFactory.getLogger("Network");
    public static final Marker NETWORK_MARKER = MarkerFactory.getMarker("ATRIBOT_NETWORK");

    private Channel channel;
    private SocketAddress address;
    private final Queue<PacketHolder> packetBuffer = new ConcurrentLinkedQueue<>();

    private final NetworkListener listener;
    private final PacketRegistry registry;
    private final boolean receiveFromServerSide;

    public Connection(NetworkListener listener, PacketRegistry registry, boolean isServerSide) {
        this.listener = listener;
        this.registry = registry;
        this.receiveFromServerSide = !isServerSide;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        channel = ctx.channel();
        address = channel.remoteAddress();
        listener.connectionOpened(this);
        flushQueue();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet msg) {
        if (NETWORK_LOGGER.isDebugEnabled())
            NETWORK_LOGGER.debug(
                NETWORK_MARKER,
                "Received packet: {}, hash = {}",
                msg.getClass().getSimpleName(),
                msg.hashCode()
            );
        listener.receivePacket(this, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!channel.isOpen())
            return;
        if (cause instanceof TimeoutException) {
            NETWORK_LOGGER.error(NETWORK_MARKER, "A client met a timeout", cause);
            channel.close().syncUninterruptibly();
            listener.fatalError(this, cause);
        } else if (cause instanceof ChannelException || cause instanceof CodecException) {
            NETWORK_LOGGER.error(NETWORK_MARKER, "Fatal error in sending/receiving packet", cause);
            channel.close().syncUninterruptibly();
            listener.fatalError(this, cause);
        } else {
            NETWORK_LOGGER.warn(NETWORK_MARKER, "An exception caught in channel", cause);
            listener.exceptionCaught(this, cause);
        }
    }

    public boolean isNotActive() {
        return channel == null || !channel.isOpen();
    }

    public boolean isConnecting() {
        return channel == null;
    }

    public void disconnect() {
        if (!isNotActive())
            channel.close().syncUninterruptibly();
    }

    public void sendPacket(Packet packet) {
        sendPacket(packet, null);
    }

    public void sendPacket(Packet msg, GenericFutureListener<? extends Future<? super Void>> listener) {
        if (NETWORK_LOGGER.isDebugEnabled())
            NETWORK_LOGGER.debug(
                NETWORK_MARKER,
                "Sending or pushing packet: {}, hash = {}",
                msg.getClass().getSimpleName(),
                msg.hashCode()
            );
        if (isNotActive()) {
            PacketHolder holder = new PacketHolder(msg, listener);
            packetBuffer.offer(holder);
        } else {
            flushQueue();
            sendPacket0(msg, listener);
        }
    }

    private void sendPacket0(Packet packet, GenericFutureListener<? extends Future<? super Void>> listener) {
        if (channel.eventLoop().inEventLoop()) {
            ChannelFuture channelFuture = channel.writeAndFlush(packet);
            if (listener != null)
                channelFuture.addListener(listener);
            channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        } else {
            channel.eventLoop().execute(() -> {
                ChannelFuture channelFuture = channel.writeAndFlush(packet);
                if (listener != null)
                    channelFuture.addListener(listener);
                channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            });
        }
    }

    private void flushQueue() {
        if (isNotActive())
            return;
        synchronized (packetBuffer) {
            while (!packetBuffer.isEmpty()) {
                PacketHolder holder = packetBuffer.poll();
                sendPacket0(holder.packet(), holder.listener());
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        listener.connectionClosed(this);
        channel = null;
        packetBuffer.clear();
    }

    public static void setupChannel(Channel channel, Connection connection) {
        channel.config().setOption(ChannelOption.TCP_NODELAY, true);
        channel
            .pipeline()
            .addLast("timeout", new ReadTimeoutHandler(30))
            .addLast("splitter", new SplitterHandler())
            .addLast("decoder", new PacketDecoder(connection))
            .addLast("prepender", new SizePrepender())
            .addLast("encoder", new PacketEncoder(connection))
            .addLast("packet_handler", connection);
    }
}
