package io.github.nickid2018.atribot.plugins.wiki.resolve;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.nickid2018.atribot.core.communicate.Communication;
import io.github.nickid2018.atribot.plugins.wiki.WikiPlugin;
import io.github.nickid2018.atribot.plugins.wiki.persist.WikiEntry;
import io.github.nickid2018.atribot.util.WebUtil;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Slf4j
@Getter
public class PageShooter {

    private static final Cache<String, String> PAGE_SHOOTER_HTML_CACHE = CacheBuilder
        .newBuilder()
        .concurrencyLevel(8)
        .maximumSize(250)
        .expireAfterAccess(60, TimeUnit.MINUTES)
        .softValues()
        .build();

    private final String url;
    private final String baseURI;
    private final String sourceHTML;

    @SneakyThrows
    public PageShooter(String url) {
        this.url = url;
        URI uri = URI.create(url);
        this.baseURI = uri.getScheme() + "://" + uri.getHost();
        this.sourceHTML = PAGE_SHOOTER_HTML_CACHE.get(url, () -> {
            HttpGet get = new HttpGet(url);
            get.addHeader("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
            return WebUtil.fetchDataInText(get, WikiInfo.ATRIBOT_WIKI_PLUGIN_UA, false);
        });
    }

    private CompletableFuture<byte[]> renderHTML(String data, String element, WikiEntry wikiEntry) {
        Map<String, Object> map = new HashMap<>(Map.of("html", data));
        if (element != null)
            map.put("element", element);
        if (wikiEntry.renderWidth != 0 && wikiEntry.renderHeight != 0)
            map.put("windowSize", IntIntPair.of(wikiEntry.renderWidth, wikiEntry.renderHeight));
        return Communication.communicateWithResult(
            "atribot-plugin-web-renderer",
            "webrenderer.render_html_element",
            map
        );
    }

    private void cleanDocument(Document doc, Element element) {
        while (!element.equals(doc.body())) {
            Element parent = element.parent();
            if (parent == null)
                break;
            for (Element child : parent.children())
                if (!child.equals(element) && !child.tagName().equals("style") && !child.tagName().equals("script"))
                    child.remove();
            element = parent;
            if (element.tagName().equals("template")) {
                Element clone = element.child(0).clone();
                parent = element.parent();
                if (parent != null)
                    parent.appendChild(clone);
                element = clone;
            }
        }

        Queue<Element> bfs = new LinkedList<>();
        bfs.offer(element);
        while (!bfs.isEmpty()) {
            Element now = bfs.poll();
            if (now.hasClass("navbox"))
                continue;
            now.removeClass("mw-collapsible");
            now.removeClass("mw-collapsed");
            now.removeClass("collapsible");
            now.removeClass("collapsed");
            now.children().forEach(bfs::offer);
        }

        doc.body().addClass("heimu_toggle_on");
        doc.head().prependChild(new Element("base").attr("href", baseURI));
        Element heimuToggle = new Element("style").text("""
                                                        body.heimu_toggle_on .heimu, body.heimu_toggle_on .heimu rt {
                                                          background-color: rgba(37,37,37,0.13) !important;
                                                        }
                                                        """);
        doc.head().appendChild(heimuToggle);
        doc.getElementsByClass("custom-modal").forEach(Element::remove);
    }

    private static final List<String> SUPPORT_INFOBOX = List.of(
        "notaninfobox", "infoboxtable", "infoboxSpecial", "infotemplatebox", "infobox2",
        "tpl-infobox", "portable-infobox", "toccolours", "infobox"
    );

    public CompletableFuture<byte[]> renderInfobox(WikiPlugin plugin, WikiEntry wikiEntry) {
        return CompletableFuture
            .supplyAsync(() -> {
                Document doc = Jsoup.parse(sourceHTML);
                Element found = SUPPORT_INFOBOX
                    .stream()
                    .map(doc::getElementsByClass)
                    .filter(Predicate.not(Elements::isEmpty))
                    .findFirst()
                    .map(Elements::first)
                    .orElse(null);

                if (found == null) {
                    log.debug("No infobox found in {}", url);
                    return null;
                }

                String className = "infobox-render-%s".formatted(RandomStringUtils.random(32, true, true));
                found.addClass(className);
                cleanDocument(doc, found);

                String html = doc.html();
                return ObjectObjectImmutablePair.of(html, "." + className);
            }, plugin.getExecutorService())
            .thenComposeAsync(
                data -> data == null
                        ? CompletableFuture.completedFuture(null)
                        : renderHTML(data.left(), data.right(), wikiEntry),
                plugin.getExecutorService()
            );
    }
}
