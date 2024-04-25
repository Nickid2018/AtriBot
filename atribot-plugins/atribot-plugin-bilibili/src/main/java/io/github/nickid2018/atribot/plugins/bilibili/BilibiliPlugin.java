package io.github.nickid2018.atribot.plugins.bilibili;

import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.plugin.AbstractAtriBotPlugin;
import io.github.nickid2018.atribot.core.plugin.PluginInfo;

public class BilibiliPlugin extends AbstractAtriBotPlugin {

    private final BilibiliReceiver receiver = new BilibiliReceiver(this);

    @Override
    public PluginInfo getPluginInfo() {
        return new PluginInfo(
            "atribot-plugin-bilibili",
            "bilibili",
            "1.0",
            "Nickid2018",
            "A plugin to get information from Bilibili"
        );
    }

    @Override
    public CommunicateReceiver getCommunicateReceiver() {
        return receiver;
    }
}
