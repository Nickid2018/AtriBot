package io.github.nickid2018.atribot.core.plugin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public class PluginInfo {

    private final String identifier;
    private final String name;
    private final String version;
    private final String author;
    private final String description;

    private final Map<String, String> anotherInfo = new HashMap<>();

    public PluginInfo addInfo(String key, String value) {
        anotherInfo.put(key, value);
        return this;
    }
}
