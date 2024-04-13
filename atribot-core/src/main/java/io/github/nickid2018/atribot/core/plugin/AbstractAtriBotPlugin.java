package io.github.nickid2018.atribot.core.plugin;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public abstract class AbstractAtriBotPlugin implements AtriBotPlugin {

    private ExecutorService executorService;

    @Override
    public void onPluginPreload() throws Exception {
        executorService = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                .setNameFormat("Plugin-" + getPluginInfo().getName() + "-%d")
                .setDaemon(true)
                .build()
        );
    }

    @Override
    public void onPluginLoad() throws Exception {
    }

    @Override
    public void onPluginUnload() throws Exception {
        executorService.shutdown();
    }
}
