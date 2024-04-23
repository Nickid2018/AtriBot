package io.github.nickid2018.atribot.core;

import io.github.nickid2018.atribot.core.communicate.Communication;
import io.github.nickid2018.atribot.core.message.MessageManager;
import io.github.nickid2018.atribot.core.plugin.PluginClassLoader;
import io.github.nickid2018.atribot.core.plugin.PluginManager;
import io.github.nickid2018.atribot.util.ClassPathDependencyResolver;
import io.github.nickid2018.atribot.util.Configuration;
import lombok.SneakyThrows;

import java.util.Scanner;

public class AtriBotMain {
    @SneakyThrows
    public static void main(String[] args) {
        if (System.getenv("DEV_PLUGIN") == null && ClassPathDependencyResolver.inProductionEnvironment(AtriBotMain.class))
            ClassPathDependencyResolver.resolveCoreDependencies();

        PluginClassLoader.preloadAllClassesForCore();
        Configuration.init();
        if (Configuration.hasKey("proxy")) {
            System.setProperty("http.proxyHost", Configuration.getStringOrElse("proxy.host", "localhost"));
            System.setProperty("http.proxyPort", String.valueOf(Configuration.getIntOrElse("proxy.port", 7890)));
            System.setProperty("https.proxyHost", Configuration.getStringOrElse("proxy.host", "localhost"));
            System.setProperty("https.proxyPort", String.valueOf(Configuration.getIntOrElse("proxy.port", 7890)));
        }

        MessageManager manager = new MessageManager();
        manager.start();

        Communication.communicate("atribot.preload_class", null);
        PluginManager.loadAll();

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            if (line.equals("exit")) {
                manager.stop();
                PluginManager.unloadAll();
                break;
            }
            try {
                if (line.equals("reload")) {
                    PluginManager.reloadAll();
                }
                if (line.equals("unload")) {
                    PluginManager.unloadAll();
                }
                if (line.startsWith("load ")) {
                    PluginManager.loadPlugin(line.substring(5));
                }
                if (line.startsWith("unload ")) {
                    PluginManager.unloadPlugin(line.substring(7));
                }
                if (line.startsWith("reload ")) {
                    PluginManager.unloadPlugin(line.substring(7));
                    PluginManager.loadPlugin(line.substring(7));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
