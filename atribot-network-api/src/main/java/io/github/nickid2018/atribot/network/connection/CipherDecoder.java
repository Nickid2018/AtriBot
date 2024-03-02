package io.github.nickid2018.atribot.network.connection;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.SneakyThrows;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.util.List;

public class CipherDecoder extends MessageToMessageDecoder<ByteBuf> {

    private final Cipher cipher;
    private byte[] cacheInput = new byte[1024];

    @SneakyThrows
    public CipherDecoder(SecretKey secretKey) {
        cipher = Cipher.getInstance("AES/CFB8/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(secretKey.getEncoded()));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        int inputSize = msg.readableBytes();
        if (cacheInput.length < inputSize)
            cacheInput = new byte[inputSize];
        msg.readBytes(cacheInput, 0, inputSize);
        ByteBuf buf = ctx.alloc().heapBuffer(cipher.getOutputSize(inputSize));
        buf.writerIndex(cipher.update(cacheInput, 0, inputSize, buf.array(), buf.arrayOffset()));
        out.add(buf);
    }
}
