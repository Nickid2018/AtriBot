package io.github.nickid2018.atribot.core.communicate;

import io.github.nickid2018.atribot.core.plugin.PluginContainer;
import io.github.nickid2018.atribot.core.plugin.PluginManager;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class Communication {

    public static <T> void communicate(String key, T obj) {
        PluginManager.forEachPluginNames(plugin -> {
            PluginContainer container = PluginManager.getPlugin(plugin);
            CommunicateReceiver receiver = container.pluginInstance().getCommunicateReceiver();
            if (receiver != null && receiver.availableCommunicateKeys().contains(key)) {
                try {
                    receiver.communicate(key, obj);
                } catch (Exception e) {
                    log.error("Error in communicating with plugin {}", plugin, e);
                }
            }
        });
    }

    public static <T, D> CompletableFuture<D> communicateWithResult(String plugin, String key, T obj) {
        PluginContainer container = PluginManager.getPlugin(plugin);
        if (container == null)
            return CompletableFuture.failedFuture(new IllegalArgumentException("Plugin not found"));
        CommunicateReceiver receiver = container.pluginInstance().getCommunicateReceiver();
        if (receiver != null && receiver.availableCommunicateKeys().contains(key)) {
            CompletableFuture<D> future;
            try {
                future = receiver.communicate(key, obj);
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
            if (future != null)
                return future;
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.failedFuture(new IllegalArgumentException("Key not found"));
    }
}
