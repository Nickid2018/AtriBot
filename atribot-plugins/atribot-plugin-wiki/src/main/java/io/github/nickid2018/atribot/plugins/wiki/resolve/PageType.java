package io.github.nickid2018.atribot.plugins.wiki.resolve;

public enum PageType {
    // Normal
    NORMAL, REDIRECT, NORMALIZED, DISAMBIGUATION, FILE_IMAGE, FILE_AUDIO, SPECIAL,
    WITH_DOCUMENT, ANONYMOUS_USER_PAGE, DIRECT_URL,
    // Error
    SECTION_NOT_FOUND, PAGE_NOT_FOUND, NETWORK_ERROR,
    // Unsupported
    UNSUPPORTED,
}
