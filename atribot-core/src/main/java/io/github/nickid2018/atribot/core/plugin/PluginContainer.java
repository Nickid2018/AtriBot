package io.github.nickid2018.atribot.core.plugin;

import lombok.AllArgsConstructor;
import lombok.Getter;

public record PluginContainer(Class<?> entranceClass, AtriBotPlugin pluginInstance, PluginClassLoader classLoader) {

}
