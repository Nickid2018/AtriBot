package io.github.nickid2018.atribot.plugins.wiki.resolve;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.nickid2018.atribot.plugins.wiki.WikiPlugin;
import io.github.nickid2018.atribot.util.FunctionUtil;
import io.github.nickid2018.atribot.util.JsonUtil;
import io.github.nickid2018.atribot.util.WebUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class InterwikiStorage {

    private final WikiPlugin plugin;

    private final Map<String, CompletableFuture<WikiInfo>> interwikisMap = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Map<String, CompletableFuture<String>>>> availableInterwikis = new ConcurrentHashMap<>();

    public InterwikiStorage(WikiPlugin plugin) {
        this.plugin = plugin;
        plugin.wikiEntries.forEach(entry -> addWiki(entry.baseURL));
    }

    public void addWiki(String baseURL) {
        CompletableFuture<WikiInfo> info = CompletableFuture.supplyAsync(
            FunctionUtil.tryUntil(() -> new WikiInfo(baseURL), 5),
            plugin.getExecutorService()
        );
        interwikisMap.put(baseURL, info);
        availableInterwikis.put(baseURL, info.thenApplyAsync(
            FunctionUtil.tryUntil(this::getInterwikis, 5),
            plugin.getExecutorService()
        ));
    }

    public CompletableFuture<WikiInfo> getWikiInfo(String startWikiURL, String interwiki) {
        if (startWikiURL == null)
            return CompletableFuture.completedFuture(null);

        if (interwiki.isEmpty()) {
            CompletableFuture<WikiInfo> info = interwikisMap.computeIfAbsent(
                startWikiURL,
                url -> CompletableFuture.supplyAsync(
                    FunctionUtil.tryUntil(() -> new WikiInfo(url), 5),
                    plugin.getExecutorService()
                )
            );
            availableInterwikis.computeIfAbsent(
                startWikiURL,
                url -> info.thenApplyAsync(
                    FunctionUtil.tryUntil(this::getInterwikis, 5),
                    plugin.getExecutorService()
                )
            );
            return info;
        }

        int index = interwiki.indexOf(':');
        String interwikiName = index > 0 ? interwiki.substring(0, index) : interwiki;
        String left = index > 0 ? interwiki.substring(index + 1) : "";

        return availableInterwikis
            .get(startWikiURL)
            .thenApplyAsync(
                interwikis -> interwikis.get(interwikiName),
                plugin.getExecutorService()
            )
            .thenComposeAsync(
                interwikiURL -> interwikiURL == null
                                ? CompletableFuture.completedFuture(null)
                                : interwikiURL.thenComposeAsync(
                                    url -> getWikiInfo(url, left),
                                    plugin.getExecutorService()
                                ),
                plugin.getExecutorService()
            );
    }

    public static final Map<String, String> INTERWIKI_QUERY = Map.of(
        "action", "query",
        "format", "json",
        "meta", "siteinfo",
        "siprop", "interwikimap"
    );

    @SneakyThrows
    public Map<String, CompletableFuture<String>> getInterwikis(WikiInfo wikiInfo) {
        Map<String, CompletableFuture<String>> map = new HashMap<>();

        HttpGet get = new HttpGet(wikiInfo.getApiURL() + WebUtil.formatQuery(INTERWIKI_QUERY));
        JsonArray object = JsonUtil.getDataInPath(
            WebUtil.fetchDataInJson(get, WikiInfo.ATRIBOT_WIKI_PLUGIN_UA).getAsJsonObject(),
            "query.interwikimap",
            JsonArray.class
        ).orElseThrow();

        for (int i = 0; i < object.size(); i++) {
            JsonObject interwiki = object.get(i).getAsJsonObject();
            String name = interwiki.get("prefix").getAsString();
            String url = interwiki.get("url").getAsString();

            HttpGet toCheck = new HttpGet(url);
            map.put(name, CompletableFuture.supplyAsync(FunctionUtil.tryUntil(() -> {
                String webContent = WebUtil.fetchDataInText(toCheck, WikiInfo.ATRIBOT_WIKI_PLUGIN_UA, true);
                Elements elements = Jsoup.parse(webContent).select("link[rel=EditURI]");
                if (!elements.isEmpty()) {
                    String href = elements.getFirst().attr("href");
                    return href.substring(0, href.indexOf("?"));
                } else
                    return url;
            }, 5), plugin.getExecutorService()));
        }

        return map;
    }
}
