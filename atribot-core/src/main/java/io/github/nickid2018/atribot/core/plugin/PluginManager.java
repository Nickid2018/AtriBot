package io.github.nickid2018.atribot.core.plugin;

import com.google.common.reflect.ClassPath;
import io.github.nickid2018.atribot.util.FunctionUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
public class PluginManager {

    public static final File PLUGIN_FOLDER = new File("plugins");
    private static final Map<String, PluginContainer> PLUGINS_MAP = new HashMap<>();

    public static void loadAll() throws IOException {
        File[] files = PLUGIN_FOLDER.listFiles();
        if (files != null) {
            Stream.of(files)
                  .filter(File::isFile)
                  .map(File::getName)
                  .filter(name -> name.endsWith(".jar"))
                  .map(s -> s.substring(0, s.length() - 4))
                  .forEach(FunctionUtil.tryOrElse(
                      PluginManager::preLoadPlugin,
                      (s, e) -> log.error("Failed to preload plugin: {}", s, e)
                  ));
        }

        if (System.getenv().containsKey("DEV_PLUGIN")) {
            log.info("Dev plugin mode enabled!");
            ClassPath
                .from(Thread.currentThread().getContextClassLoader())
                .getAllClasses()
                .stream()
                .filter(clzInfo -> clzInfo.getPackageName().startsWith("io.github.nickid2018.atribot.plugin"))
                .map(ClassPath.ClassInfo::load)
                .filter(AtriBotPlugin.class::isAssignableFrom)
                .forEach(FunctionUtil.tryOrElse(
                    clazz -> {
                        AtriBotPlugin plugin = (AtriBotPlugin) clazz.getConstructor().newInstance();
                        log.info("Add dev plugin: {}", clazz.getSimpleName());
                        plugin.onPluginPreload();
                        PluginInfo info = plugin.getPluginInfo();
                        PLUGINS_MAP.put(info.getIdentifier(), new PluginContainer(clazz, plugin, null, true));
                    },
                    (clazz, e) -> log.error("Failed to preload plugin: {}", clazz.getName(), e)
                ));
        }

        log.info("All plugins preloaded! List of plugins: ");
        PLUGINS_MAP
            .entrySet()
            .stream()
            .map(en -> en.getKey() + (en.getValue().devPlugin() ? "[dev]" : ""))
            .map(t -> "\t- " + t)
            .forEach(log::info);

        for (Map.Entry<String, PluginContainer> entry : PLUGINS_MAP.entrySet()) {
            PluginContainer container = entry.getValue();
            try {
                container.pluginInstance().onPluginLoad();
            } catch (Throwable t) {
                log.error("Failed to load plugin: {}", entry.getKey(), t);
                try {
                    unloadPlugin(entry.getKey());
                } catch (Throwable ignored) {
                }
            }
        }

        log.info("All plugins loaded!");
    }

    public static void preLoadPlugin(String name) throws Exception {
        File file = new File(PLUGIN_FOLDER, name + ".jar");
        if (!file.exists() || !file.isFile())
            throw new IOException("Plugin not found: " + name);
        PluginClassLoader classLoader = new PluginClassLoader(file, log::debug, log::info, log::error);
        try (JarFile jarFile = new JarFile(file)) {
            List<? extends Class<?>> plugins = StreamSupport
                .stream(jarFile.stream().spliterator(), false)
                .map(JarEntry::getName)
                .filter(s -> s.endsWith(".class"))
                .map(s -> s.substring(0, s.length() - 6).replace('/', '.'))
                .map(FunctionUtil.tryOrElse(classLoader::loadClass, c -> null))
                .filter(Objects::nonNull)
                .filter(AtriBotPlugin.class::isAssignableFrom)
                .toList();
            if (plugins.isEmpty())
                throw new IOException("No plugin found in the jar file: " + name);
            if (plugins.size() > 1)
                throw new IOException("Multiple plugins found in the jar file: " + name);
            AtriBotPlugin plugin = (AtriBotPlugin) plugins.getFirst().getConstructor().newInstance();
            log.info("Add plugin: {}", name);
            plugin.onPluginPreload();
            PluginInfo info = plugin.getPluginInfo();
            PLUGINS_MAP.put(info.getIdentifier(), new PluginContainer(plugins.getFirst(), plugin, classLoader, false));
        }
    }

    public static void loadPlugin(String name) throws Exception {
        preLoadPlugin(name);
        PluginContainer container = PLUGINS_MAP.get(name);
        container.pluginInstance().onPluginLoad();
    }

    public static void unloadPlugin(String name) throws Exception {
        PluginContainer container = PLUGINS_MAP.remove(name);
        try {
            container.pluginInstance().onPluginUnload();
        } catch (Throwable t) {
            log.error("Failed to unload plugin: {}", name, t);
        } finally {
            if (container.classLoader() != null)
                container.classLoader().close();
        }
    }

    public static PluginContainer getPlugin(String name) {
        return PLUGINS_MAP.get(name);
    }

    public static void forEachPluginNames(Consumer<String> consumer) {
        PLUGINS_MAP.keySet().forEach(consumer);
    }

    public static void forEachPluginNamesOutPlace(Consumer<String> consumer) {
        Set.copyOf(PLUGINS_MAP.keySet()).forEach(consumer);
    }

    public static void reloadPlugin(String name) throws Exception {
        unloadPlugin(name);
        preLoadPlugin(name);
    }

    public static void reloadAll() throws IOException {
        unloadAll();
        loadAll();
    }

    public static void unloadAll() {
        forEachPluginNamesOutPlace(FunctionUtil.tryOrElse(
            PluginManager::unloadPlugin,
            (s, e) -> log.error("Failed to unload plugin: {}", s, e)
        ));
    }
}
