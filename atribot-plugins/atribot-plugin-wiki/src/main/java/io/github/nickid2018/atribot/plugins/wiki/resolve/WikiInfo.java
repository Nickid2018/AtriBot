package io.github.nickid2018.atribot.plugins.wiki.resolve;

import com.google.gson.*;
import io.github.nickid2018.atribot.util.JsonUtil;
import io.github.nickid2018.atribot.util.WebUtil;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;

import java.net.URI;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Slf4j
@Getter
public class WikiInfo {

    public static final Map<String, String> META_INFO_QUERY = Map.of(
        "action", "query",
        "format", "json",
        "meta", "siteinfo",
        "siprop", "extensions|general|namespaces|namespacealiases"
    );

    public static final Map<String, String> PAGE_QUERY = Map.of(
        "action", "query",
        "format", "json",
        "prop", "info|imageinfo|pageprops",
        "ppprop", "disambiguation",
        "iiprop", "url|mime",
        "redirects", "1"
    );

    public static final Map<String, String> PAGE_TEXT_EXTRACTS = Map.of(
        "action", "query",
        "format", "json",
        "prop", "extracts",
        "exchars", "200",
        "exsectionformat", "plain",
        "explaintext", "1",
        "exintro", "1"
    );

    public static final Map<String, String> PAGE_PARSE = Map.of(
        "action", "parse",
        "prop", "text"
    );

    public static final Map<String, String> PAGE_PARSE_SECTIONS = Map.of(
        "action", "parse",
        "prop", "sections"
    );

    public static final Predicate<String> ANONYMOUS_USER_PAGE = Pattern
        .compile("\\d{1,3}(\\.\\d{1,3}){3}")
        .asMatchPredicate();

    public static final String ATRIBOT_WIKI_PLUGIN_UA = "Atribot Wiki Plugin/1.0";

    private final String apiURL;
    private boolean isWiki = true;

    private final String articleURL;
    private final String scriptURL;
    private final String mainPageName;

    private final Set<String> namespaces = new HashSet<>();
    private final Set<String> extensions = new HashSet<>();

    @SneakyThrows
    public WikiInfo(String apiURL) {
        this.apiURL = apiURL;

        String siteInfoStr = WebUtil.fetchDataInText(
            new HttpGet(apiURL + WebUtil.formatQuery(META_INFO_QUERY)),
            ATRIBOT_WIKI_PLUGIN_UA,
            true
        );
        JsonObject siteInfo;
        try {
            siteInfo = JsonParser.parseString(siteInfoStr).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            isWiki = false;
            articleURL = apiURL;
            scriptURL = apiURL;
            mainPageName = null;
            return;
        }

        JsonObject query = siteInfo.getAsJsonObject("query");
        mainPageName = JsonUtil.getStringInPathOrNull(query, "general.mainpage");
        String server = JsonUtil.getStringInPathOrNull(query, "general.server");
        URI serverURI = new URI(server);
        articleURL = serverURI.resolve(JsonUtil.getStringInPathOrElse(query, "general.articlepath", "")).toString();
        scriptURL = serverURI.resolve(JsonUtil.getStringInPathOrElse(query, "general.script", "")).toString();

        JsonArray extensionsArray = query.getAsJsonArray("extensions");
        for (JsonElement element : extensionsArray)
            extensions.add(element.getAsJsonObject().get("name").getAsString());

        JsonObject namespacesObject = query.getAsJsonObject("namespaces");
        Map<String, JsonElement> namespacesMap = namespacesObject.asMap();
        for (JsonElement element : namespacesMap.values())
            namespaces.add(element.getAsJsonObject().get("*").getAsString());
        JsonArray namespaceAliasesArray = query.getAsJsonArray("namespacealiases");
        for (JsonElement element : namespaceAliasesArray)
            namespaces.add(element.getAsJsonObject().get("*").getAsString());
    }

    public String resolveArticleURL(String title, String section) {
        return articleURL.replace("$1", title + (section == null || section.isEmpty() ? "" : ("#" + section)));
    }

    public String resolveScriptURL(int pageID, String section) {
        return scriptURL + "?curid=" + pageID + (section == null || section.isEmpty() ? "" : ("#" + section));
    }

    public PageInfo parsePageInfo(String namespace, String title, String section) {
        if (!isWiki)
            return PageInfo.directURL(resolveArticleURL(title, section));
        if (title == null || title.isEmpty())
            return parsePageInfo(null, mainPageName, null);
        if (namespace != null && !namespaces.contains(namespace))
            return PageInfo.pageNotFound(namespace, new String[0]);

        String searchTitle = namespace == null || namespace.isEmpty() ? title : namespace + ":" + title;

        if (ANONYMOUS_USER_PAGE.test(title))
            return PageInfo.anonymousUserPage(searchTitle);

        try {
            Map<String, String> query = new HashMap<>(PAGE_QUERY);
            query.put("titles", searchTitle);
            HttpGet get = new HttpGet(apiURL + WebUtil.formatQuery(query));
            JsonObject pageData = WebUtil.fetchDataInJson(get, ATRIBOT_WIKI_PLUGIN_UA, false).getAsJsonObject();

            JsonObject queryData = pageData.getAsJsonObject("query");
            JsonObject pages = queryData.getAsJsonObject("pages");
            if (pages.isEmpty())
                return PageInfo.pageNotFound(searchTitle, searchTitle(searchTitle));

            JsonObject page = pages.entrySet().iterator().next().getValue().getAsJsonObject();
            if (page.has("missing"))
                return PageInfo.pageNotFound(searchTitle, searchTitle(searchTitle));
            if (page.has("special"))
                return PageInfo.specialPage(searchTitle, resolveArticleURL(searchTitle, null));

            int pageID = page.get("pageid").getAsInt();
            String source = searchTitle + (section == null || section.isEmpty() ? "" : ("#" + section));

            if (queryData.has("redirects")) {
                JsonObject redirect = queryData.getAsJsonArray("redirects").get(0).getAsJsonObject();
                String to = redirect.get("to").getAsString();
                String toFragment = redirect.has("tofragment") ? redirect.get("tofragment").getAsString() : section;
                int namespaceSplit = to.indexOf(':');
                if (namespaceSplit != -1) {
                    namespace = to.substring(0, namespaceSplit);
                    to = to.substring(namespaceSplit + 1);
                }
                PageInfo info = parsePageInfo(namespace, to, toFragment);
                Map<String, Object> infoData = info.data();

                if (info.type() == PageType.REDIRECT)
                    return PageInfo.redirectPage(
                        source,
                        (String) infoData.get("pageNameRedirected"),
                        resolveScriptURL(pageID, section),
                        info,
                        true
                    );
                else {
                    String redirected = (String) infoData.get("pageName");
                    if (info.type() == PageType.NORMAL) {
                        String sectionRedirect = (String) infoData.get("pageSection");
                        if (sectionRedirect != null && !sectionRedirect.isEmpty())
                            redirected += "#" + sectionRedirect;
                    }

                    return PageInfo.redirectPage(
                        source,
                        redirected,
                        resolveScriptURL(pageID, section),
                        info,
                        false
                    );
                }
            }

            if (queryData.has("normalized")) {
                JsonObject normalized = queryData.getAsJsonArray("normalized").get(0).getAsJsonObject();
                String to = normalized.get("to").getAsString();
                int namespaceSplit = to.indexOf(':');
                if (namespaceSplit != -1) {
                    namespace = to.substring(0, namespaceSplit);
                    to = to.substring(namespaceSplit + 1);
                }
                PageInfo info = parsePageInfo(namespace, to, section);
                Map<String, Object> infoData = info.data();

                if (info.type() == PageType.REDIRECT)
                    return PageInfo.redirectPage(
                        source,
                        (String) infoData.get("pageNameRedirected"),
                        resolveScriptURL(pageID, section),
                        info,
                        (boolean) infoData.get("multipleRedirects")
                    );
                else {
                    String redirected = (String) infoData.get("pageName");
                    if (info.type() == PageType.NORMAL) {
                        String sectionRedirect = (String) infoData.get("pageSection");
                        if (sectionRedirect != null && !sectionRedirect.isEmpty())
                            redirected += "#" + sectionRedirect;
                    }

                    return PageInfo.normalizePage(
                        source,
                        redirected,
                        resolveScriptURL(pageID, section),
                        info
                    );
                }
            }

            if (page.has("pageprops") && page.getAsJsonObject("pageprops").has("disambiguation"))
                return PageInfo.disambiguationPage(searchTitle, resolveScriptURL(pageID, section));

            if (page.has("imageinfo")) {
                JsonArray imageInfo = page.getAsJsonArray("imageinfo");
                List<Pair<String, String>> fileAndMIME = imageInfo
                    .asList().stream()
                    .map(JsonElement::getAsJsonObject)
                    .<Pair<String, String>>map(info -> {
                        String url = info.get("url").getAsString();
                        String mime = info.get("mime").getAsString();
                        return new ObjectObjectImmutablePair<>(url, mime);
                    })
                    .toList();
                return PageInfo.filePage(searchTitle, resolveScriptURL(pageID, section), fileAndMIME);
            }

            if (extensions.contains("TextExtracts") && (section == null || section.isEmpty())) {
                Map<String, String> extractQuery = new HashMap<>(PAGE_TEXT_EXTRACTS);
                extractQuery.put("titles", searchTitle);
                HttpGet extractGet = new HttpGet(apiURL + WebUtil.formatQuery(extractQuery));
                JsonObject pageExtracts = WebUtil
                    .fetchDataInJson(extractGet, ATRIBOT_WIKI_PLUGIN_UA, false)
                    .getAsJsonObject();
                JsonObject pagesExtracts = JsonUtil
                    .getDataInPath(pageExtracts, "query.pages", JsonObject.class)
                    .orElseThrow();
                JsonObject pageExtract = pagesExtracts.entrySet().iterator().next().getValue().getAsJsonObject();
                String extract = pageExtract.get("extract").getAsString();
                return PageInfo.normalPage(searchTitle, section, resolveScriptURL(pageID, null), extract);
            }

            Map<String, String> parseQuery = new HashMap<>(PAGE_PARSE);
            parseQuery.put("page", searchTitle);

            if (section != null && !section.isEmpty()) {
                Map<String, String> sectionQuery = new HashMap<>(PAGE_PARSE_SECTIONS);
                sectionQuery.put("page", searchTitle);
                HttpGet sectionGet = new HttpGet(apiURL + WebUtil.formatQuery(sectionQuery));
                JsonObject pageSections = WebUtil
                    .fetchDataInJson(sectionGet, ATRIBOT_WIKI_PLUGIN_UA, false)
                    .getAsJsonObject();

                JsonArray sections = JsonUtil
                    .getDataInPath(pageSections, "parse.sections", JsonArray.class)
                    .orElseThrow();
                Map<String, String> sectionMap = new HashMap<>();
                sections.forEach(element -> {
                    JsonObject sectionObject = element.getAsJsonObject();
                    String sectionName = sectionObject.get("anchor").getAsString();
                    String sectionIndex = sectionObject.get("index").getAsString();
                    sectionMap.put(sectionName, sectionIndex);
                });

                if (sectionMap.containsKey(section)) {
                    parseQuery.put("section", sectionMap.get(section));
                } else {
                    String[] availableSections = sectionMap.keySet().toArray(new String[0]);
                    return PageInfo.sectionNotFound(searchTitle, resolveScriptURL(pageID, null), availableSections);
                }
            }

            HttpGet parseGet = new HttpGet(apiURL + WebUtil.formatQuery(parseQuery));
            JsonObject pageParse = WebUtil.fetchDataInJson(parseGet, ATRIBOT_WIKI_PLUGIN_UA, false).getAsJsonObject();
            String parse = JsonUtil.getStringInPath(pageParse, "parse.wikitext.*").orElseThrow();
            if (section != null && !section.isEmpty())
                parse = parse.substring(parse.indexOf('\n'));
            parse = parse.substring(0, parse.indexOf('\n'));
            return PageInfo.normalPage(searchTitle, section, resolveScriptURL(pageID, section), parse);
        } catch (Exception e) {
            return PageInfo.networkError(searchTitle, e);
        }
    }

    public String[] searchTitle(String title) {
        return new String[0];
    }
}
