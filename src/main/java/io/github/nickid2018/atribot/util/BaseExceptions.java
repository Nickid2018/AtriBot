package io.github.nickid2018.atribot.util;

public class BaseExceptions {

    public static final String GENERAL_TIMEOUT_DESC = "AtribotGeneral-Timeout";
    public static final RuntimeException GENERAL_TIMEOUT_EXCEPTION = new RuntimeException(GENERAL_TIMEOUT_DESC);

    public static boolean isGeneralTimeout(Throwable e) {
        return e instanceof RuntimeException && e.getMessage().equals(GENERAL_TIMEOUT_DESC);
    }
}
