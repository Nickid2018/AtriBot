package io.github.nickid2018.atribot.plugins.wiki.resolve;

public enum PageType {
    NORMAL, REDIRECT, NORMALIZED, DISAMBIGUATION, FILE_IMAGE, FILE_AUDIO, SPECIAL, ANONYMOUS_USER_PAGE, DIRECT_URL, // Normal
    SECTION_NOT_FOUND, PAGE_NOT_FOUND, NETWORK_ERROR, // Error
    UNSUPPORTED, // Unsupported
}
