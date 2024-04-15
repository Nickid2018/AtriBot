package io.github.nickid2018.atribot.core.plugin;

public record PluginContainer(
    Class<?> entranceClass, AtriBotPlugin pluginInstance, PluginClassLoader classLoader, boolean devPlugin
) {
}
