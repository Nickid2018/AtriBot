package io.github.nickid2018.atribot.network.connection;

import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.Getter;

@Getter
public class PacketEncoder extends MessageToByteEncoder<Packet> {
    private final Connection connection;
    public PacketEncoder(Connection connection) {
        this.connection = connection;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, ByteBuf out) throws Exception {
        int packetID = connection.getRegistry().getPacketId(msg);
        if (packetID < 0)
            throw new ChannelException("Unknown packet [%s]".formatted(msg.getClass().getName()));
        PacketBuffer buf = new PacketBuffer(out);
        buf.writeVarInt(packetID);
        msg.serializeToStream(buf);
    }
}
