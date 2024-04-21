package io.github.nickid2018.atribot.core.message;

import io.github.nickid2018.atribot.network.file.TransferFileResolver;
import lombok.AllArgsConstructor;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@AllArgsConstructor
public class FileTransfer {

    private final MessageManager manager;

    public CompletableFuture<String> sendFile(String backend, File file, ExecutorService executorService) {
        Map<String, String> info = manager.getBackendInformation().get(backend);
        if (info == null)
            return CompletableFuture.failedFuture(new IllegalArgumentException("Backend not found!"));
        String transferType = info.getOrDefault("ingoingTransferType", "file");
        String transferArg = info.getOrDefault("ingoingTransferArg", ".=>.");
        return TransferFileResolver.transferFile(transferType, transferArg, file, executorService);
    }

    public CompletableFuture<String> sendFile(String backend, InputStream stream, ExecutorService executorService) {
        Map<String, String> info = manager.getBackendInformation().get(backend);
        if (info == null)
            return CompletableFuture.failedFuture(new IllegalArgumentException("Backend not found!"));
        String transferType = info.getOrDefault("ingoingTransferType", "file");
        String transferArg = info.getOrDefault("ingoingTransferArg", ".=>.");
        return TransferFileResolver.transferFile(transferType, transferArg, stream, executorService);
    }
}
