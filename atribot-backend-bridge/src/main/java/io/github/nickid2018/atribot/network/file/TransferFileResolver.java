package io.github.nickid2018.atribot.network.file;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.nickid2018.atribot.network.connection.Connection;
import io.github.nickid2018.atribot.util.FunctionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TransferFileResolver {

    private static final Cache<String, String> FILE_TRANSFER_CACHE = CacheBuilder
        .newBuilder()
        .concurrencyLevel(8)
        .maximumSize(250)
        .expireAfterWrite(60, TimeUnit.MINUTES)
        .<String, String>removalListener(notification -> {
            File file = new File(notification.getValue());
            if (file.exists())
                file.delete();
        })
        .softValues()
        .build();

    public static CompletableFuture<String> transferFile(String transferType, String transferArg, File data, ExecutorService executorService) {
        switch (transferType) {
            case "file" -> {
                String[] transferArgs = transferArg.split("=>");
                if (transferArgs.length != 2)
                    return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid transfer argument!"));
                File transferFile = transferArgs[0].equals(".") ? data.getParentFile() : new File(transferArgs[0]);
                if (!transferFile.isDirectory())
                    transferFile.mkdirs();
                String randomName = RandomStringUtils.random(64, true, true);
                File targetFile = transferArgs[0].equals(".") ? data : new File(transferFile, randomName);
                File remoteFile = transferArgs[1].equals(".") ? data : new File(transferArgs[1], randomName);
                return CompletableFuture.supplyAsync(
                    FunctionUtil.sneakyThrowsSupplier(() -> {
                        if (!data.equals(targetFile)) {
                            targetFile.deleteOnExit();
                            FILE_TRANSFER_CACHE.put(randomName, targetFile.getAbsolutePath());
                            IOUtils.copy(data.toURI().toURL(), targetFile);
                        }
                        String uri = remoteFile.toURI().toString();
                        log.info(
                            Connection.NETWORK_MARKER,
                            "File transfer (file input): {} -> {}",
                            targetFile.getAbsolutePath(),
                            uri
                        );
                        return uri;
                    }), executorService
                );
            }
            default -> {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Unknown transfer type!"));
            }
        }
    }

    public static CompletableFuture<String> transferFile(String transferType, String transferArg, InputStream data, ExecutorService executorService) {
        switch (transferType) {
            case "file" -> {
                String[] transferArgs = transferArg.split("=>");
                boolean isFirstDot = transferArgs[0].equals(".");
                boolean isSecondDot = transferArgs[1].equals(".");
                if (transferArgs.length != 2)
                    return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid transfer argument!"));
                if (isFirstDot) {
                    transferArgs[0] = System.getProperty("java.io.tmpdir");
                } else {
                    File transferFile = new File(transferArgs[0]);
                    if (!transferFile.isDirectory())
                        transferFile.mkdirs();
                }
                String randomName = RandomStringUtils.random(64, true, true);
                File targetFile = new File(transferArgs[0], randomName);

                String remoteDir = isFirstDot && isSecondDot ? transferArgs[0] : transferArgs[1];
                File remoteFile = new File(remoteDir, randomName);
                return CompletableFuture.supplyAsync(
                    FunctionUtil.sneakyThrowsSupplier(() -> {
                        OutputStream os = new FileOutputStream(targetFile);
                        IOUtils.copy(data, os);
                        os.close();
                        targetFile.deleteOnExit();
                        FILE_TRANSFER_CACHE.put(randomName, targetFile.getAbsolutePath());
                        String uri = remoteFile.toURI().toString();
                        log.info(
                            Connection.NETWORK_MARKER,
                            "File transfer (stream input): {} -> {}",
                            targetFile.getAbsolutePath(),
                            uri
                        );
                        return uri;
                    }), executorService
                );
            }
            default -> {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Unknown transfer type!"));
            }
        }
    }
}
