package io.github.nickid2018.atribot.plugins.wakatime;

import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.communicate.Communication;
import io.github.nickid2018.atribot.core.plugin.AbstractAtriBotPlugin;
import io.github.nickid2018.atribot.core.plugin.PluginInfo;
import io.github.nickid2018.atribot.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@SuppressWarnings("unused")
public class WakatimePlugin extends AbstractAtriBotPlugin {

    private static final Logger log = LoggerFactory.getLogger(WakatimePlugin.class);
    private final WakatimeReceiver receiver = new WakatimeReceiver(this);
    public boolean oauth2ServiceAvailable;

    @Override
    public PluginInfo getPluginInfo() {
        return new PluginInfo(
            "atribot-plugin-wakatime",
            "Wakatime",
            "1.0",
            "Nickid2018",
            " A plugin for Wakatime"
        );
    }

    @Override
    public CommunicateReceiver getCommunicateReceiver() {
        return receiver;
    }

    @Override
    public void onPluginLoad() throws Exception {
        registerOAuth2Service();
    }

    public void registerOAuth2Service() {
        if (oauth2ServiceAvailable)
            return;
        Communication.communicateWithResult("atribot-plugin-oauth2-service", "oauth2.register", Map.of(
            "oauthName", "wakatime",
            "authenticateURL", "https://wakatime.com/oauth/authorize",
            "tokenGrantURL", "https://wakatime.com/oauth/token",
            "revokeURL", "https://wakatime.com/oauth/revoke",
            "refreshTokenEnabled", true,
            "redirect", "/wakaTimeOAuth",
            "clientID", Configuration.getStringOrElse("wakatime.client_id", ""),
            "clientSecret", Configuration.getStringOrElse("wakatime.client_secret", ""),
            "uriAppend", true
        )).thenAccept(result -> oauth2ServiceAvailable = true).exceptionally(e -> {
            log.error("Failed to register Wakatime OAuth 2.0 service", e);
            oauth2ServiceAvailable = false;
            return null;
        });
    }
}
