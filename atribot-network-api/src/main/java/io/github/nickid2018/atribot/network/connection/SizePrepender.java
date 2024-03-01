package io.github.nickid2018.atribot.network.connection;

import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@ChannelHandler.Sharable
public class SizePrepender extends MessageToByteEncoder<ByteBuf> {

    protected void encode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, ByteBuf byteBuf2) {
        int i = byteBuf.readableBytes();
        int j = PacketBuffer.getVarIntSize(i);
        if (j > 3) {
            throw new IllegalArgumentException("unable to fit " + i + " into 3");
        } else {
            PacketBuffer packetByteBuf = new PacketBuffer(byteBuf2);
            packetByteBuf.ensureWritable(j + i);
            packetByteBuf.writeVarInt(i);
            packetByteBuf.writeBytes(byteBuf, byteBuf.readerIndex(), i);
        }
    }
}
