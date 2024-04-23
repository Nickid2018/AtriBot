package io.github.nickid2018.atribot.plugins.wiki.resolve;

import it.unimi.dsi.fastutil.Pair;

import java.util.List;
import java.util.Map;

public record PageInfo(PageType type, Map<String, Object> data) {

    public static PageInfo normalPage(String pageName, String pageSection, String sectionIndex, String pageURL, String content) {
        return new PageInfo(PageType.NORMAL, Map.of(
            "pageName", pageName,
            "pageSection", pageSection == null ? "" : pageSection,
            "pageSectionIndex", sectionIndex == null ? "" : sectionIndex,
            "pageURL", pageURL,
            "content", content
        ));
    }

    public static PageInfo normalizePage(String pageNameSource, String pageNameRedirected, String pageURL, PageInfo pageInfo) {
        return new PageInfo(PageType.NORMALIZED, Map.of(
            "pageNameSource", pageNameSource,
            "pageNameRedirected", pageNameRedirected,
            "pageInfo", pageInfo
        ));
    }

    public static PageInfo redirectPage(String pageNameSource, String pageNameRedirected, String pageURL, PageInfo pageInfo, boolean multipleRedirects) {
        return new PageInfo(PageType.REDIRECT, Map.of(
            "pageNameSource", pageNameSource,
            "pageNameRedirected", pageNameRedirected,
            "pageInfo", pageInfo,
            "multipleRedirects", multipleRedirects
        ));
    }

    public static PageInfo disambiguationPage(String pageName, String pageURL) {
        return new PageInfo(PageType.DISAMBIGUATION, Map.of(
            "pageName", pageName,
            "pageURL", pageURL
        ));
    }

    public static PageInfo filePage(String pageName, String pageURL, List<Pair<String, String>> fileAndMIME) {
        return new PageInfo(PageType.UNSUPPORTED, Map.of(
            "pageName", pageName,
            "pageURL", pageURL,
            "fileAndMIME", fileAndMIME
        ));
    }

    public static PageInfo specialPage(String pageName, String pageURL) {
        return new PageInfo(PageType.SPECIAL, Map.of(
            "pageName", pageName,
            "pageURL", pageURL
        ));
    }

    public static PageInfo anonymousUserPage(String pageName) {
        return new PageInfo(PageType.ANONYMOUS_USER_PAGE, Map.of(
            "pageName", pageName
        ));
    }

    public static PageInfo directURL(String url) {
        return new PageInfo(PageType.DIRECT_URL, Map.of(
            "url", url
        ));
    }

    public static PageInfo sectionNotFound(String pageName, String pageURL, String[] availableSections) {
        return new PageInfo(PageType.SECTION_NOT_FOUND, Map.of(
            "pageName", pageName,
            "pageURL", pageURL,
            "availableSections", availableSections
        ));
    }

    public static PageInfo pageNotFound(String pageName, String[] pageSuggestions) {
        return new PageInfo(PageType.PAGE_NOT_FOUND, Map.of(
            "pageName", pageName,
            "pageSuggestions", pageSuggestions
        ));
    }

    public static PageInfo networkError(String pageName, Throwable t) {
        return new PageInfo(PageType.NETWORK_ERROR, Map.of(
            "pageName", pageName,
            "error", t
        ));
    }
}
