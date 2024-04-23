package io.github.nickid2018.atribot.util;

import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.logging.LogManager;

public class LogUtil {

    public static void redirectJULToSLF4J() {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();
    }
}
