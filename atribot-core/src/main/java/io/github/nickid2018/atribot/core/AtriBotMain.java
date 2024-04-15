package io.github.nickid2018.atribot.core;

import io.github.nickid2018.atribot.core.communicate.Communication;
import io.github.nickid2018.atribot.core.message.MessageManager;
import io.github.nickid2018.atribot.core.plugin.PluginManager;
import io.github.nickid2018.atribot.util.ClassPathDependencyResolver;
import io.github.nickid2018.atribot.util.Configuration;
import lombok.SneakyThrows;

import java.io.File;
import java.util.Scanner;


public class AtriBotMain {

    public static final File LIBRARY_PATH;

    static {
        String libraryPath = System.getenv("LIBRARY_PATH");
        if (libraryPath == null)
            libraryPath = "/libraries";
        LIBRARY_PATH = new File(libraryPath);
    }

    @SneakyThrows
    public static void main(String[] args) {
        if (System.getenv("DEV_PLUGIN") == null && ClassPathDependencyResolver.inProductionEnvironment())
            ClassPathDependencyResolver.resolveCoreDependencies();

        Configuration.init();
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
