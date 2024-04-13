package io.github.nickid2018.atribot.core;

import io.github.nickid2018.atribot.core.communicate.Communication;
import io.github.nickid2018.atribot.core.message.MessageManager;
import io.github.nickid2018.atribot.core.plugin.PluginManager;
import io.github.nickid2018.atribot.util.Configuration;
import lombok.SneakyThrows;

import java.util.Scanner;


public class AtriBotMain {

    @SneakyThrows
    public static void main(String[] args) {
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
        }
    }
}
