package io.github.nickid2018.atribot.plugins.mcping;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

public class ForgeDataResolver {

    public static ByteBuf toBuf(String source) {
        int size0 = source.charAt(0);
        int size1 = source.charAt(1);
        int size = size0 | size1 << 15;
        ByteBuf buf = Unpooled.buffer(size);
        int stringIndex = 2;
        int buffer = 0;
        int bitsInBuf;
        for(bitsInBuf = 0; stringIndex < source.length(); ++stringIndex) {
            while(bitsInBuf >= 8) {
                buf.writeByte(buffer);
                buffer >>>= 8;
                bitsInBuf -= 8;
            }
            char c = source.charAt(stringIndex);
            buffer |= (c & 32767) << bitsInBuf;
            bitsInBuf += 15;
        }
        while(buf.readableBytes() < size) {
            buf.writeByte(buffer);
            buffer >>>= 8;
            bitsInBuf -= 8;
        }
        return buf;
    }

    public static int readVarInt(ByteBuf buf) {
        int i = 0;
        int j = 0;
        while (true) {
            int k = buf.readByte();
            i |= (k & 0x7F) << j++ * 7;
            if (j > 5)
                throw new IllegalArgumentException("VarInt too big");
            if ((k & 0x80) != 128) break;
        }
        return i;
    }

    public static String readUtf(ByteBuf buf) {
        int size = readVarInt(buf);
        int readableBytes = buf.readableBytes();
        if (size > readableBytes) {
            throw new IllegalArgumentException(STR."Not enough bytes in buffer, expected \{size}, but got \{readableBytes}");
        }
        String string = buf.toString(buf.readerIndex(), size, StandardCharsets.UTF_8);
        buf.readerIndex(buf.readerIndex() + size);
        return string;
    }
}
