package io.github.nickid2018.atribot.network.connection;

import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import lombok.Getter;

import java.util.List;

@Getter
public class PacketDecoder extends ByteToMessageDecoder {

    private final Connection connection;

    public PacketDecoder(Connection connection) {
        this.connection = connection;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() == 0)
            return;
        if (connection.isNotActive())
            return;
        PacketBuffer buf = new PacketBuffer(in);
        int id = buf.readVarInt();
        Packet packet = connection.getRegistry().createPacket(id, connection.isReceiveFromServerSide());
        packet.deserializeFromStream(buf);
        if (buf.readableBytes() > 0)
            throw new DecoderException("Bad packet [%s] - Unexpected %s byte(s) at the packet tail".formatted(
                packet.getClass().getName(),
                buf.readableBytes()
            ));
        out.add(packet);
    }
}
