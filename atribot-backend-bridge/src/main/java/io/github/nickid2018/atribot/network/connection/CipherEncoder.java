package io.github.nickid2018.atribot.network.connection;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.SneakyThrows;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class CipherEncoder extends MessageToByteEncoder<ByteBuf> {

    private final Cipher cipher;
    private byte[] cacheInput = new byte[1024];
    private byte[] cacheOutput = new byte[1024];

    @SneakyThrows
    public CipherEncoder(SecretKey secretKey) {
        cipher = Cipher.getInstance("AES/CFB8/NoPadding");
        this.cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(secretKey.getEncoded()));
    }

    protected void encode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, ByteBuf byteBuf2) throws Exception {
        int inputSize = byteBuf.readableBytes();
        if (cacheInput.length < inputSize)
            cacheInput = new byte[inputSize];
        byteBuf.readBytes(this.cacheInput, 0, inputSize);
        int outputSize = this.cipher.getOutputSize(inputSize);
        if (cacheOutput.length < outputSize)
            cacheOutput = new byte[outputSize];
        byteBuf2.writeBytes(cacheOutput, 0, this.cipher.update(cacheInput, 0, inputSize, cacheOutput));
    }
}
