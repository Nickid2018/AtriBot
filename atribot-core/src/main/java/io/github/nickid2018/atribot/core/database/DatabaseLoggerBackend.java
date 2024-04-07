package io.github.nickid2018.atribot.core.database;

import com.j256.ormlite.logger.Level;
import com.j256.ormlite.logger.LogBackend;
import com.j256.ormlite.logger.LoggerFactory;
import lombok.AllArgsConstructor;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

@AllArgsConstructor
public class DatabaseLoggerBackend implements LogBackend {

    public static void init() {
        ILoggerFactory loggerFactory = org.slf4j.LoggerFactory.getILoggerFactory();
        LoggerFactory.setLogBackendFactory(className -> new DatabaseLoggerBackend(loggerFactory.getLogger(className)));
    }

    public static final Marker DATABASE_MARKER = MarkerFactory.getMarker("ATRIBOT_DATABASE");

    private Logger logger;

    @Override
    public boolean isLevelEnabled(Level level) {
        return switch (level) {
            case TRACE -> logger.isTraceEnabled();
            case DEBUG -> logger.isDebugEnabled();
            case INFO -> logger.isInfoEnabled();
            case WARNING -> logger.isWarnEnabled();
            case ERROR, FATAL -> logger.isErrorEnabled();
            default -> false;
        };
    }

    @Override
    public void log(Level level, String s) {
        switch (level) {
            case TRACE -> logger.trace(DATABASE_MARKER, s);
            case DEBUG -> logger.debug(DATABASE_MARKER, s);
            case WARNING -> logger.warn(DATABASE_MARKER, s);
            case ERROR, FATAL -> logger.error(DATABASE_MARKER, s);
            default -> logger.info(DATABASE_MARKER, s);
        }
    }

    @Override
    public void log(Level level, String s, Throwable throwable) {
        switch (level) {
            case TRACE -> logger.trace(DATABASE_MARKER, s, throwable);
            case DEBUG -> logger.debug(DATABASE_MARKER, s, throwable);
            case WARNING -> logger.warn(DATABASE_MARKER, s, throwable);
            case ERROR, FATAL -> logger.error(DATABASE_MARKER, s, throwable);
            default -> logger.info(DATABASE_MARKER, s, throwable);
        }
    }
}
