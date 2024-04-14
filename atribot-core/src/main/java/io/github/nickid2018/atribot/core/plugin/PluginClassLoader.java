package io.github.nickid2018.atribot.core.plugin;

import io.github.nickid2018.atribot.util.ClassPathDependencyResolver;

import java.io.*;
import java.net.URLClassLoader;
import java.util.function.Consumer;

public class PluginClassLoader extends URLClassLoader {

    public PluginClassLoader(File jarFile, Consumer<String> debugService, Consumer<String> loggerService, Consumer<String> errorService) throws IOException {
        super(
            ClassPathDependencyResolver.resolveDependencies(jarFile, debugService, loggerService, errorService),
            Thread.currentThread().getContextClassLoader()
        );
        ClassLoader.registerAsParallelCapable();
    }
}
