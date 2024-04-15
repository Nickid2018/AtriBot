package io.github.nickid2018.atribot.plugins.oauth2;

import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.communicate.Communication;
import io.github.nickid2018.atribot.core.database.DatabaseManager;
import io.github.nickid2018.atribot.core.plugin.AbstractAtriBotPlugin;
import io.github.nickid2018.atribot.core.plugin.PluginInfo;

public class OAuth2Plugin extends AbstractAtriBotPlugin {

    public DatabaseManager databaseManager;
    public OAuth2Server server = new OAuth2Server();
    public OAuth2Receiver receiver = new OAuth2Receiver(this, server);

    @Override
    public PluginInfo getPluginInfo() {
        return new PluginInfo(
            "atribot-plugin-oauth2-service",
            "OAuth2",
            "1.0",
            "Nickid2018",
            "A plugin for OAuth2"
        );
    }

    @Override
    public CommunicateReceiver getCommunicateReceiver() {
        return receiver;
    }

    @Override
    public void onPluginPreload() throws Exception {
        super.onPluginPreload();
        databaseManager = new DatabaseManager("database/oauth2.db");
        server.startServer();
    }

    @Override
    public void onPluginLoad() {
        Communication.communicate("oauth2.service.started", null);
    }

    @Override
    public void onPluginUnload() throws Exception {
        server.stopServer();
        databaseManager.close();
        Communication.communicate("oauth2.service.stopped", null);
        super.onPluginUnload();
    }
}
