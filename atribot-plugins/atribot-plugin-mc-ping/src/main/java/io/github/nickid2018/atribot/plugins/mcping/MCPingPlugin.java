package io.github.nickid2018.atribot.plugins.mcping;

import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.plugin.AbstractAtriBotPlugin;
import io.github.nickid2018.atribot.core.plugin.PluginInfo;

public class MCPingPlugin extends AbstractAtriBotPlugin {

    private final MCPingReceiver receiver = new MCPingReceiver(this);

    @Override
    public PluginInfo getPluginInfo() {
        return new PluginInfo(
            "atribot-plugin-mc-ping",
            "Minecraft Server Ping",
            "1.0",
            "Nickid2018",
            "Provide Minecraft server ping service"
        );
    }

    @Override
    public CommunicateReceiver getCommunicateReceiver() {
        return receiver;
    }
}
