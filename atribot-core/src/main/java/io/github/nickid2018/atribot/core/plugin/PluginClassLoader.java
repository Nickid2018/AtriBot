package io.github.nickid2018.atribot.core.plugin;

import com.google.common.reflect.ClassPath;
import io.github.nickid2018.atribot.util.ClassPathDependencyResolver;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URLClassLoader;
import java.util.function.Consumer;

@Slf4j
public class PluginClassLoader extends URLClassLoader {

    public PluginClassLoader(File jarFile, Consumer<String> debugService, Consumer<String> loggerService, Consumer<String> errorService) throws IOException {
        super(
            ClassPathDependencyResolver.resolveDependencies(jarFile, debugService, loggerService, errorService),
            Thread.currentThread().getContextClassLoader()
        );
        ClassLoader.registerAsParallelCapable();
    }

    @SneakyThrows
    public static void preloadAllClassesForCore() {
        ClassPath.from(Thread.currentThread().getContextClassLoader())
            .getTopLevelClassesRecursive("io.github.nickid2018.atribot")
            .forEach(clzInfo -> {
                try {
                    clzInfo.load();
                } catch (Exception e) {
                    log.error("Error in preloading class {}", clzInfo.getName(), e);
                }
            });
    }
}
