package io.github.nickid2018.atribot.plugins.qrcode;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import io.github.nickid2018.atribot.core.communicate.Communicate;
import io.github.nickid2018.atribot.core.communicate.CommunicateFilter;
import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.message.CommandCommunicateData;
import io.github.nickid2018.atribot.network.message.ImageMessage;
import io.github.nickid2018.atribot.network.message.MessageChain;
import io.github.nickid2018.atribot.network.packet.backend.ImageResolveStartPacket;
import io.github.nickid2018.atribot.util.FunctionUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class QRCodeReceiver implements CommunicateReceiver {

    private final QRCodePlugin plugin;

    @Communicate("command.normal")
    @CommunicateFilter(key = "name", value = "qr-encode")
    public void doEncode(CommandCommunicateData commandData) {
        String toEncode = String.join(" ", commandData.commandArgs);
        CompletableFuture.supplyAsync(FunctionUtil.sneakyThrowsSupplier(() -> {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(toEncode, BarcodeFormat.QR_CODE, 256, 256, Map.of(
                EncodeHintType.MARGIN, 16,
                EncodeHintType.CHARACTER_SET, "UTF-8"
            ));
            return MatrixToImageWriter.toBufferedImage(matrix);
        }), plugin.getExecutorService()).thenCompose(FunctionUtil.sneakyThrowsFunc(bufferedImage -> {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", stream);
            return commandData.messageManager.getFileTransfer().sendFile(
                commandData.backendID,
                new ByteArrayInputStream(stream.toByteArray()),
                plugin.getExecutorService()
            );
        })).thenAccept(fileID -> {
            if (fileID == null)
                return;
            commandData.messageManager.sendMessage(
                commandData.backendID,
                commandData.targetData,
                new MessageChain().next(new ImageMessage("", URI.create(fileID)))
            );
        }).exceptionally(e -> {
            commandData.messageManager.sendMessage(
                commandData.backendID,
                commandData.targetData,
                MessageChain.text("QRCode：编码时出现错误，错误报告如下\n" + e.getMessage())
            );
            return null;
        });
    }

    @Communicate("command.normal")
    @CommunicateFilter(key = "name", value = "qr-decode")
    public void doDecode(CommandCommunicateData commandData) {
        MessageChain chain = commandData.messageChain;
        ImageMessage imageMessage = chain
            .getMessages()
            .stream()
            .filter(ImageMessage.class::isInstance)
            .map(ImageMessage.class::cast)
            .findFirst()
            .orElse(null);

        if (imageMessage == null) {
            plugin.getExecutorService().execute(() -> commandData.messageManager.sendMessage(
                commandData.backendID,
                commandData.targetData,
                MessageChain.text("QRCode：未找到图片信息")
            ));
            return;
        }

        plugin.getExecutorService().execute(() -> commandData.messageManager.reactionMessage(
            commandData.backendID,
            commandData.messageChain,
            "waiting"
        ));

        URI imageURI = imageMessage.getResolved();
        if (imageURI == null)
            commandData.messageManager
                .getTransactionQueue(commandData.backendID)
                .startTransaction(
                    new ImageResolveStartPacket(Set.of(imageMessage.getImgKey())),
                    packet -> packet
                        .getImageMessage()
                        .stream()
                        .map(ImageMessage::getResolved)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .ifPresent(uri -> doDecode0(uri, commandData))
                );
        else
            doDecode0(imageURI, commandData);
    }

    private void doDecode0(URI resolved, CommandCommunicateData commandData) {
        CompletableFuture.supplyAsync(FunctionUtil.sneakyThrowsSupplier(() -> {
            BufferedImage image = ImageIO.read(resolved.toURL());
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            return new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(source)));
        }), plugin.getExecutorService()).thenAccept(result -> {
            if (result == null)
                commandData.messageManager.sendMessage(
                    commandData.backendID,
                    commandData.targetData,
                    MessageChain.text("QRCode：未能识别图片中的二维码")
                );
            else {
                String res = result.getText();
                long time = result.getTimestamp();
                Map<ResultMetadataType, Object> metadata = result.getResultMetadata();
                String metadataStr = metadata
                    .entrySet().stream()
                    .filter(e -> e.getKey() != ResultMetadataType.BYTE_SEGMENTS)
                    .map(e -> e.getKey().name() + "=" + e.getValue())
                    .collect(Collectors.joining("\n"));
                String resStr = "内容：" + res + "\n时间：" + new Date(time) + "\n元数据：\n" + metadataStr;
                commandData.messageManager.sendMessage(
                    commandData.backendID,
                    commandData.targetData,
                    MessageChain.text(resStr)
                );
            }
        }).exceptionally(e -> {
            commandData.messageManager.sendMessage(
                commandData.backendID,
                commandData.targetData,
                MessageChain.text("QRCode：解码时出现错误，错误报告如下\n" + e.getMessage())
            );
            return null;
        });
    }
}
