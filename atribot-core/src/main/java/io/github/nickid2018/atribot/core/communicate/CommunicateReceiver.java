package io.github.nickid2018.atribot.core.communicate;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface CommunicateReceiver {

    <T, D> CompletableFuture<T> communicate(String communicateKey, D data) throws Exception;

    Set<String> availableCommunicateKeys();

    CommunicateReceiver NOP = new CommunicateReceiver() {
        @Override
        public <T, D> CompletableFuture<T> communicate(String communicateKey, D data) {
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Set<String> availableCommunicateKeys() {
            return Collections.EMPTY_SET;
        }
    };
}
