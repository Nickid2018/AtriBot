package io.github.nickid2018.atribot.network.connection;

import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

import java.util.List;

public class SplitterHandler extends ByteToMessageDecoder {

    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        byteBuf.markReaderIndex();
        byte[] bs = new byte[3];
        for (int i = 0; i < bs.length; ++i) {
            if (!byteBuf.isReadable()) {
                byteBuf.resetReaderIndex();
                return;
            }
            bs[i] = byteBuf.readByte();
            if (bs[i] >= 0) {
                PacketBuffer packetByteBuf = new PacketBuffer(Unpooled.wrappedBuffer(bs));
                try {
                    int j = packetByteBuf.readVarInt();
                    if (byteBuf.readableBytes() < j) {
                        byteBuf.resetReaderIndex();
                        return;
                    }
                    list.add(byteBuf.readBytes(j));
                } finally {
                    packetByteBuf.release();
                }
                return;
            }
        }
        throw new CorruptedFrameException("length wider than 21-bit");
    }
}

