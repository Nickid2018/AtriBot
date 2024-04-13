package io.github.nickid2018.atribot.plugin.testing;

import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.plugin.AtriBotPlugin;
import io.github.nickid2018.atribot.core.plugin.PluginInfo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("unused")
public class TestPlugin implements AtriBotPlugin {

    @Override
    public PluginInfo getPluginInfo() {
        return new PluginInfo("Test Plugin", "1.0", "Nickid2018", "Test plugin for AtriBot");
    }

    @Override
    public CommunicateReceiver getCommunicateReceiver() {
        return CommunicateReceiver.NOP;
    }

    @Override
    public void onPluginPreload() {
        log.info("Test plugin preloaded!");
    }

    @Override
    public void onPluginLoad() {
        log.info("Test plugin loaded!");
    }

    @Override
    public void onPluginUnload() {
        log.info("Test plugin unloaded!");
    }
}