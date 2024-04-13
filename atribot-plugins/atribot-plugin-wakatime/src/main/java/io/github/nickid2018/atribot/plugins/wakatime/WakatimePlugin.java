package io.github.nickid2018.atribot.plugins.wakatime;

import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.communicate.Communication;
import io.github.nickid2018.atribot.core.plugin.AbstractAtriBotPlugin;
import io.github.nickid2018.atribot.core.plugin.PluginInfo;
import io.github.nickid2018.atribot.util.Configuration;

import java.util.Map;

@SuppressWarnings("unused")
public class WakatimePlugin extends AbstractAtriBotPlugin {

    private final WakatimeReceiver receiver = new WakatimeReceiver(this);

    @Override
    public PluginInfo getPluginInfo() {
        return new PluginInfo("Wakatime", "1.0", "Nickid2018", "A plugin for Wakatime");
    }

    @Override
    public CommunicateReceiver getCommunicateReceiver() {
        return receiver;
    }

    @Override
    public void onPluginLoad() {
        Communication.communicate("oauth2.register", Map.of(
            "oauthName", "wakatime",
            "authenticateURL", "https://wakatime.com/oauth/authorize",
            "tokenGrantURL", "https://wakatime.com/oauth/token",
            "revokeURL", "https://wakatime.com/oauth/revoke",
            "refreshTokenEnabled", true,
            "redirect", "/wakaTimeOAuth",
            "clientID", Configuration.getStringOrElse("wakatime.client_id", ""),
            "clientSecret", Configuration.getStringOrElse("wakatime.client_secret", ""),
            "uriAppend", true
        ));
    }
}
