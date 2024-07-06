package io.github.nickid2018.atribot.plugins.permission;

import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.plugin.AbstractAtriBotPlugin;
import io.github.nickid2018.atribot.core.plugin.PluginInfo;

public class PermissionPlugin extends AbstractAtriBotPlugin {

    private final PermissionReceiver receiver = new PermissionReceiver(this);

    @Override
    public PluginInfo getPluginInfo() {
        return new PluginInfo(
            "atribot-plugin-permission-utils",
            "Permission Utils",
            "1.0",
            "Nickid2018",
            "A plugin to manage permissions."
        );
    }

    @Override
    public CommunicateReceiver getCommunicateReceiver() {
        return receiver;
    }
}
