package io.github.nickid2018.atribot.core.plugin;

import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;

public interface AtriBotPlugin {

    PluginInfo getPluginInfo();

    CommunicateReceiver getCommunicateReceiver();

    void onPluginPreload() throws Exception;

    void onPluginLoad() throws Exception;

    void onPluginUnload() throws Exception;
}
