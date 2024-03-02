package io.github.nickid2018.atribot.core.plugin;

import io.github.nickid2018.atribot.util.FunctionUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
public class PluginManager {

    public static final File PLUGIN_FOLDER = new File("plugins");
    private static final Map<String, PluginContainer> PLUGINS_MAP = new HashMap<>();

    public static void loadAll() {
        File[] files = PLUGIN_FOLDER.listFiles();
        if (files == null)
            return;
        Stream.of(files)
                .filter(File::isFile)
                .map(File::getName)
                .filter(name -> name.endsWith(".jar"))
                .map(s -> s.substring(0, s.length() - 4))
                .forEach(FunctionUtils.noExceptionOrElse(
                        PluginManager::loadPlugin,
                        (s, e) -> log.error("Failed to load plugin: " + s, e)
                ));
    }

    public static void loadPlugin(String name) throws Exception {
        File file = new File(PLUGIN_FOLDER, name + ".jar");
        if (!file.exists() || !file.isFile())
            throw new IOException("Plugin not found: " + name);
        PluginClassLoader classLoader = new PluginClassLoader(file);
        try (JarFile jarFile = new JarFile(file)) {
            List<? extends Class<?>> plugins = StreamSupport.stream(jarFile.stream().spliterator(), false)
                    .map(JarEntry::getName)
                    .filter(s -> s.endsWith(".class"))
                    .map(s -> s.substring(0, s.length() - 6).replace('/', '.'))
                    .map(FunctionUtils.noException(classLoader::loadClass))
                    .filter(AtriBotPlugin.class::isAssignableFrom)
                    .toList();
            if (plugins.isEmpty())
                throw new IOException("No plugin found in the jar file: " + name);
            if (plugins.size() > 1)
                throw new IOException("Multiple plugins found in the jar file: " + name);
            AtriBotPlugin plugin = (AtriBotPlugin) plugins.get(0).getConstructor().newInstance();
            plugin.onPluginLoad();
            PLUGINS_MAP.put(name, new PluginContainer(plugins.get(0), plugin, classLoader));
        }
    }

    public static void loadDevEnvironmentPlugin(String name, String className) throws Exception {
        Class<?> pluginClass = Class.forName(className);
        AtriBotPlugin plugin = (AtriBotPlugin) pluginClass.getConstructor().newInstance();
        PLUGINS_MAP.put(name, new PluginContainer(pluginClass, plugin, null));
    }

    public static void unloadPlugin(String name) throws Exception {
        PluginContainer container = PLUGINS_MAP.remove(name);
        container.pluginInstance().onPluginUnload();
        if (container.classLoader() != null)
            container.classLoader().close();
    }

    public static void forEachPluginNames(Consumer<String> consumer) {
        PLUGINS_MAP.keySet().forEach(consumer);
    }

    public static void forEachPluginNamesOutPlace(Consumer<String> consumer) {
        Set.copyOf(PLUGINS_MAP.keySet()).forEach(consumer);
    }

    public static void reloadPlugin(String name) throws Exception {
        unloadPlugin(name);
        loadPlugin(name);
    }

    public static void reloadAll() {
        forEachPluginNamesOutPlace(FunctionUtils.noExceptionOrElse(
                PluginManager::unloadPlugin,
                (s, e) -> log.error("Failed to unload plugin: " + s, e)
        ));
        loadAll();
    }
}
