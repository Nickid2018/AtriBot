package io.github.nickid2018.atribot.plugins.oauth2;

import com.j256.ormlite.dao.Dao;
import io.github.nickid2018.atribot.core.communicate.Communicate;
import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.communicate.Communication;
import io.github.nickid2018.atribot.core.message.MessageManager;
import io.github.nickid2018.atribot.network.message.TargetData;
import lombok.AllArgsConstructor;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
public class OAuth2Receiver implements CommunicateReceiver {

    private final OAuth2Plugin plugin;
    private final Map<String, OAuth2Authenticator> authenticators = new HashMap<>();

    @Communicate("server.started")
    public void startServer() {
        Communication.communicate("oauth2.service.started");
    }

    @Communicate("server.stopped")
    public void stopServer() {
        Communication.communicate("oauth2.service.stopped");
        authenticators.values().forEach(OAuth2Authenticator::completeWaiting);
        authenticators.clear();
    }

    @Communicate("oauth2.register")
    public void registerOAuth2(Map<String, Object> data) throws SQLException {
        String oauthName = (String) data.get("oauthName");
        String authenticateURL = (String) data.get("authenticateURL");
        String tokenGrantURL = (String) data.get("tokenGrantURL");
        String revokeURL = (String) data.get("revokeURL");
        boolean refreshTokenEnabled = (boolean) data.get("refreshTokenEnabled");
        String redirect = STR."/oauth\{data.get("redirect")}";
        String clientID = (String) data.get("clientID");
        String clientSecret = (String) data.get("clientSecret");
        boolean uriAppend = (boolean) data.get("uriAppend");

        Dao<AuthenticateToken, String> tokenDao = plugin.databaseManager.getTable(oauthName, AuthenticateToken.class);
        OAuth2Authenticator authenticator = new OAuth2Authenticator(
            plugin,
            authenticateURL,
            tokenGrantURL,
            revokeURL,
            refreshTokenEnabled,
            redirect,
            clientID,
            clientSecret,
            uriAppend,
            tokenDao
        );

        Communication.communicate("server.register", redirect, authenticator);
        authenticators.put(oauthName, authenticator);
    }

    @Communicate("oauth2.unregister")
    public void unregisterOAuth2(String oauthName) {
        OAuth2Authenticator authenticator = authenticators.remove(oauthName);
        if (authenticator == null)
            return;
        authenticator.completeWaiting();
        Communication.communicate("server.unregister", authenticator.getRedirect());
    }

    @SuppressWarnings("unchecked")
    @Communicate("oauth2.authenticate")
    public CompletableFuture<?> authenticate(Map<String, Object> data) {
        String oauthName = (String) data.get("oauthName");
        String backendID = (String) data.get("backendID");
        TargetData targetData = (TargetData) data.get("target");
        MessageManager manager = (MessageManager) data.get("manager");
        if (!targetData.isUserSpecified())
            return CompletableFuture.failedFuture(new IllegalArgumentException("Target not specified"));

        List<String> scopes = (List<String>) data.getOrDefault("scopes", Collections.EMPTY_LIST);
        Map<String, String> additionalParam = (Map<String, String>) data.getOrDefault(
            "additionalParam",
            Collections.EMPTY_MAP
        );

        OAuth2Authenticator authenticator = authenticators.get(oauthName);
        if (authenticator == null)
            return CompletableFuture.failedFuture(new IllegalArgumentException("OAuth2 not registered"));

        return authenticator.authenticate(
            backendID,
            targetData,
            manager,
            scopes,
            additionalParam
        );
    }

    @Communicate("oauth2.revoke")
    public CompletableFuture<?> revoke(Map<String, Object> data) {
        String oauthName = (String) data.get("oauthName");
        String id = (String) data.get("id");

        OAuth2Authenticator authenticator = authenticators.get(oauthName);
        if (authenticator == null)
            return CompletableFuture.failedFuture(new IllegalArgumentException("OAuth2 not registered"));

        return authenticator.revoke(id);
    }
}
