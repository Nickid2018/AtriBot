package io.github.nickid2018.atribot.core.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

public class PluginClassLoader extends URLClassLoader {

    public PluginClassLoader(File jarFile) throws IOException {
        super(resolveDependencies(jarFile), PluginClassLoader.class.getClassLoader());
        ClassLoader.registerAsParallelCapable();
    }

    private static URL[] resolveDependencies(File jarFile) throws IOException {
        Set<URL> urls = new HashSet<>();
        urls.add(jarFile.toURI().toURL());
        return urls.toArray(URL[]::new);
    }
}
