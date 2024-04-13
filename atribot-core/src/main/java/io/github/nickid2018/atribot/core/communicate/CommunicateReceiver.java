package io.github.nickid2018.atribot.core.communicate;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public interface CommunicateReceiver {

    <T, D> CompletableFuture<T> communicate(String communicateKey, D data) throws Exception;

    Set<String> availableCommunicateKeys();

    CommunicateReceiver NOP = new CommunicateReceiver() {
        @Override
        public <T, D> CompletableFuture<T> communicate(String communicateKey, D data) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public Set<String> availableCommunicateKeys() {
            return Set.of();
        }
    };
}
