package io.github.nickid2018.atribot.util;

import lombok.extern.slf4j.Slf4j;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
public class Configuration {

    public static final File[] CONFIG_FILES = new File[]{
            new File("config.yml"),
            new File("config.yaml")
    };

    public static final Load YAML_LOADER = new Load(LoadSettings.builder().build());

    private static Map<String, Object> config;

    private static File chooseConfig() {
        for (File file : CONFIG_FILES)
            if (file.exists())
                return file;
        return null;
    }

    @SuppressWarnings("unchecked")
    public static void init() {
        File choose = chooseConfig();
        if (choose == null) {
            log.error("No configuration file found!");
            System.exit(1);
        }

        log.info("Using configuration file: " + choose.getName());
        try (FileReader reader = new FileReader(choose)) {
            config = (Map<String, Object>) YAML_LOADER.loadFromReader(reader);
        } catch (Exception e) {
            log.error("Error loading configuration file!", e);
            System.exit(1);
        }
    }

    @SuppressWarnings("rawtypes")
    public static Object getObject(String key) {
        String[] paths = key.split("\\.");
        Object current = config;
        for (String path : paths) {
            if (current instanceof Map map) {
                current = map.get(path);
            } else if (current instanceof List list) {
                int index = Integer.parseInt(path);
                current = list.get(index);
            } else
                return null;
        }
        return current;
    }

    public static int getIntOrElse(String key, int defaultValue) {
        Object obj = getObject(key);
        return obj instanceof Integer ? (int) obj : defaultValue;
    }

    public static <E extends Throwable> int getIntOrThrow(String key, Supplier<E> supplier) throws E {
        Object obj = getObject(key);
        if (obj instanceof Integer)
            return (int) obj;
        throw supplier.get();
    }

    public static String getString(String key) {
        return (String) getObject(key);
    }

    public static String getStringOrElse(String key, String defaultValue) {
        Object obj = getObject(key);
        return obj instanceof String ? (String) obj : defaultValue;
    }

    public static <E extends Throwable> String getStringOrThrow(String key, Supplier<E> supplier) throws E {
        Object obj = getObject(key);
        if (obj instanceof String)
            return (String) obj;
        throw supplier.get();
    }
}
