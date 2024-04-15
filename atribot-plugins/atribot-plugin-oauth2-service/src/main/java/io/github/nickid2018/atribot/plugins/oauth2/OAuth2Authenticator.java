package io.github.nickid2018.atribot.plugins.oauth2;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.google.gson.JsonObject;
import com.j256.ormlite.dao.Dao;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.nickid2018.atribot.core.message.MessageManager;
import io.github.nickid2018.atribot.network.message.MessageChain;
import io.github.nickid2018.atribot.network.message.TargetData;
import io.github.nickid2018.atribot.util.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Getter
@RequiredArgsConstructor
public class OAuth2Authenticator implements HttpHandler {

    private final OAuth2Plugin plugin;

    private final String authenticateURL;
    private final String tokenGrantURL;
    private final String revokeURL;
    private final boolean refreshTokenEnabled;
    private final String redirect;
    private final String clientID;
    private final String clientSecret;
    private final boolean uriAppend;
    private final Dao<AuthenticateToken, String> tokenDao;

    private final Map<String, Triple<String, List<String>, CompletableFuture<String>>> authSequence = new ConcurrentHashMap<>();

    @Override
    @SneakyThrows
    public void handle(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> args = Arrays
            .stream(query.split("&"))
            .map(s -> s.split("=", 2))
            .collect(
                HashMap::new, (map, strArray) -> map.put(strArray[0], strArray[1]), HashMap::putAll
            );
        String state = args.get("state");

        if (authSequence.containsKey(state)) {
            String success = """
                             <!DOCTYPE HTML><body><div style="text-align: center">Authenticated successfully</body>
                             """;
            exchange.sendResponseHeaders(200, success.length());
            exchange.getResponseBody().write(success.getBytes(StandardCharsets.UTF_8));
            exchange.close();

            String code = args.get("code");
            HttpPost post = new HttpPost(tokenGrantURL);
            post.setHeader("Accept", "application/json");

            List<NameValuePair> pairs = new ArrayList<>();
            if (uriAppend) {
                String localIP = Configuration.getStringOrElse("network.local_ip", "localhost");
                int serverPort = Configuration.getIntOrElse("oauth2.server.port", 8080);
                pairs.add(new BasicNameValuePair(
                    "redirect_uri",
                    "http://%s:%d%s".formatted(localIP, serverPort, redirect)
                ));
            }
            pairs.add(new BasicNameValuePair("code", code));
            pairs.add(new BasicNameValuePair("client_id", clientID));
            pairs.add(new BasicNameValuePair("client_secret", clientSecret));
            pairs.add(new BasicNameValuePair("grant_type", "authorization_code"));

            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8);
            post.setEntity(entity);

            JsonObject object = WebUtil.fetchDataInJson(post).getAsJsonObject();
            String accessToken = JsonUtil.getStringOrNull(object, "access_token");
            Triple<String, List<String>, CompletableFuture<String>> authData = authSequence.remove(state);

            long expiredTime = JsonUtil.getIntOrZero(object, "expires_in") * 1000L + System.currentTimeMillis();
            AuthenticateToken token = new AuthenticateToken(
                authData.getLeft(),
                accessToken,
                expiredTime,
                JsonUtil.getStringOrNull(object, "refresh_token"),
                String.join(";", authData.getMiddle())
            );
            tokenDao.createOrUpdate(token);
            authData.getRight().complete(accessToken);
        } else {
            exchange.sendResponseHeaders(400, 0);
            exchange.close();
        }
    }

    public CompletableFuture<String> authenticate(
        String backendID, TargetData target, MessageManager manager, List<String> scopes, Map<String, String> extraParameters
    ) {
        try {
            boolean entryExists = tokenDao.idExists(target.getTargetUser());
            AuthenticateToken token = entryExists ? tokenDao.queryForId(target.getTargetUser()) : null;
            boolean scopeContains = entryExists && new HashSet<>(List.of(token.scopes.split(";"))).containsAll(scopes);

            if (scopeContains && token.expireTime > System.currentTimeMillis())
                return CompletableFuture.completedFuture(token.accessToken);

            if (refreshTokenEnabled && scopeContains) {
                HttpPost post = new HttpPost(tokenGrantURL);
                post.setHeader("Accept", "application/json");

                List<NameValuePair> pairs = new ArrayList<>();
                pairs.add(new BasicNameValuePair("client_id", clientID));
                pairs.add(new BasicNameValuePair("client_secret", clientSecret));
                pairs.add(new BasicNameValuePair("grant_type", "refresh_token"));
                pairs.add(new BasicNameValuePair("refresh_token", token.refreshToken));

                if (uriAppend) {
                    String localIP = Configuration.getStringOrElse("network.local_ip", "localhost");
                    int serverPort = Configuration.getIntOrElse("oauth2.server.port", 8080);
                    pairs.add(new BasicNameValuePair(
                        "redirect_uri",
                        "http://%s:%d%s".formatted(localIP, serverPort, redirect)
                    ));
                }

                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8);
                post.setEntity(entity);

                return CompletableFuture
                    .supplyAsync(FunctionUtil.noException(() -> {
                        JsonObject object = WebUtil.fetchDataInJson(post).getAsJsonObject();
                        String accessToken = JsonUtil.getStringOrNull(object, "access_token");
                        String refreshToken = JsonUtil.getStringOrNull(object, "refresh_token");
                        long expiredTime = JsonUtil.getIntOrZero(
                            object,
                            "expires_in"
                        ) * 1000L + System.currentTimeMillis();
                        AuthenticateToken refreshed = new AuthenticateToken(
                            token.id,
                            accessToken,
                            expiredTime,
                            refreshToken,
                            token.scopes
                        );
                        tokenDao.createOrUpdate(refreshed);
                        return accessToken;
                    }), plugin.getExecutorService()).exceptionallyAsync(FunctionUtil.noException(t -> {
                        return authenticateCode(backendID, target, manager, scopes, extraParameters).get();
                    }), plugin.getExecutorService());
            }
            return authenticateCode(backendID, target, manager, scopes, extraParameters);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<String> authenticateCode(
        String backendID, TargetData target, MessageManager manager, List<String> scopes, Map<String, String> extraParameters
    ) {
        Escaper escaper = UrlEscapers.urlFormParameterEscaper();
        String state = RandomStringUtils.random(32, true, true);
        String url = "%s?client_id=%s&state=%s&scope=%s&response_type=code".formatted(
            authenticateURL, clientID, state, escaper.escape(String.join(",", scopes))
        );

        if (uriAppend) {
            String localIP = Configuration.getStringOrElse("network.local_ip", "localhost");
            int serverPort = Configuration.getIntOrElse("oauth2.server.port", 8080);
            url += "&redirect_uri=http://%s:%d%s".formatted(localIP, serverPort, redirect);
        }

        String extra = extraParameters
            .entrySet().stream()
            .map(en -> escaper.escape(en.getKey()) + "=" + escaper.escape(en.getValue()))
            .collect(Collectors.joining("&"));
        if (!extra.isEmpty())
            url += "&" + extra;

        CompletableFuture<String> future = new CompletableFuture<String>()
            .orTimeout(10, TimeUnit.MINUTES)
            .exceptionallyAsync(t -> {
                if (t instanceof TimeoutException) {
                    authSequence.remove(state);
                    throw BaseExceptions.GENERAL_TIMEOUT_EXCEPTION;
                } else
                    throw new RuntimeException("Impossible error", t);
            }, plugin.getExecutorService());
        authSequence.put(state, Triple.of(target.getTargetUser(), scopes, future));
        manager.sendMessage(
            backendID,
            target,
            MessageChain.text("Please use following link to authenticate:\n" + url)
        );
        return future;
    }

    @SneakyThrows
    public CompletableFuture<Void> revoke(String id) {
        if (revokeURL == null)
            return CompletableFuture.failedFuture(new UnsupportedOperationException("Revoke not supported"));

        boolean entryExists = tokenDao.idExists(id);
        if (!entryExists)
            return CompletableFuture.failedFuture(new IllegalArgumentException("Token not found"));
        AuthenticateToken token = tokenDao.queryForId(id);

        return CompletableFuture.runAsync(FunctionUtil.noException(() -> {
            HttpPost post = new HttpPost(revokeURL);
            post.setHeader("Accept", "application/json");

            List<NameValuePair> pairs = new ArrayList<>();
            pairs.add(new BasicNameValuePair("client_id", clientID));
            pairs.add(new BasicNameValuePair("client_secret", clientSecret));
            pairs.add(new BasicNameValuePair("token", token.accessToken));

            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8);
            post.setEntity(entity);

            WebUtil.sendNeedCode(post, 200);
            tokenDao.deleteById(id);
        }), plugin.getExecutorService());
    }

    public void completeWaiting() {
        authSequence
            .values()
            .stream()
            .map(Triple::getRight)
            .forEach(future -> future.completeExceptionally(new IOException("OAuth2 Service is shutting down")));
    }
}
