package io.github.nickid2018.atribot.plugins.oauth2;

import com.j256.ormlite.dao.Dao;
import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.message.MessageManager;
import io.github.nickid2018.atribot.network.message.TargetData;
import lombok.AllArgsConstructor;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
public class OAuth2Receiver implements CommunicateReceiver {

    private static final Set<String> communicateKeys = Set.of(
        "oauth2.register",
        "oauth2.authenticate",
        "oauth2.revoke"
    );

    private final OAuth2Plugin plugin;
    private final OAuth2Server server;
    private final Map<String, OAuth2Authenticator> authenticators = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T, D> CompletableFuture<T> communicate(String communicateKey, D data) throws Exception {
        return switch (communicateKey) {
            case "oauth2.register" -> registerOAuth2((Map<String, Object>) data);
            case "oauth2.authenticate" -> authenticate((Map<String, Object>) data);
            case "oauth2.revoke" -> revoke((Map<String, Object>) data);
            default -> null;
        };
    }

    private <T> CompletableFuture<T> registerOAuth2(Map<String, Object> data) throws SQLException {
        String oauthName = (String) data.get("oauthName");
        String authenticateURL = (String) data.get("authenticateURL");
        String tokenGrantURL = (String) data.get("tokenGrantURL");
        String revokeURL = (String) data.get("revokeURL");
        boolean refreshTokenEnabled = (boolean) data.get("refreshTokenEnabled");
        String redirect = (String) data.get("redirect");
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

        server.addHandler(redirect, authenticator);
        authenticators.put(oauthName, authenticator);
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> authenticate(Map<String, Object> data) {
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

        return (CompletableFuture<T>) authenticator.authenticate(
            backendID,
            targetData,
            manager,
            scopes,
            additionalParam
        );
    }

    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> revoke(Map<String, Object> data) {
        String oauthName = (String) data.get("oauthName");
        String id = (String) data.get("id");

        OAuth2Authenticator authenticator = authenticators.get(oauthName);
        if (authenticator == null)
            return CompletableFuture.failedFuture(new IllegalArgumentException("OAuth2 not registered"));

        return (CompletableFuture<T>) authenticator.revoke(id);
    }

    @Override
    public Set<String> availableCommunicateKeys() {
        return communicateKeys;
    }
}
