package io.github.nickid2018.atribot.core.communicate;

import java.util.Set;
import java.util.concurrent.Future;

public interface CommunicateReceiver {

    <T, D> Future<T> communicate(String communicateKey, D data);

    Set<String> availableCommunicateKeys();
}
