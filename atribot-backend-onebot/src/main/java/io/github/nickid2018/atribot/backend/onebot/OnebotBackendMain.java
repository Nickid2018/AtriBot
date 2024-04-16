package io.github.nickid2018.atribot.backend.onebot;

import cn.evole.onebot.client.OneBotClient;
import cn.evole.onebot.client.core.BotConfig;
import io.github.nickid2018.atribot.network.BackendClient;
import io.github.nickid2018.atribot.network.PacketRegister;
import io.github.nickid2018.atribot.network.message.TransactionQueue;
import io.github.nickid2018.atribot.util.ClassPathDependencyResolver;
import io.github.nickid2018.atribot.util.Configuration;
import lombok.SneakyThrows;

import java.net.InetAddress;
import java.util.Scanner;

public class OnebotBackendMain {

    @SneakyThrows
    public static void main(String[] args) {
        if (ClassPathDependencyResolver.inProductionEnvironment(OnebotBackendMain.class))
            ClassPathDependencyResolver.resolveCoreDependencies();

        Configuration.init();

        BotConfig config = new BotConfig();
        config.setReconnect(true);
        OneBotClient client = OneBotClient.create(config);

        OnebotBackendListener listener = new OnebotBackendListener(client);
        BackendClient backendClient = new BackendClient(listener);
        listener.setClient(backendClient);
        listener.setTransactionQueue(new TransactionQueue(backendClient::getConnection));
        PacketRegister.registerBackendPackets(backendClient);
        backendClient.setAutoReconnect(true);
        backendClient.connect(
            InetAddress.getByName(Configuration.getStringOrElse("network.host", "localhost")),
            Configuration.getIntOrElse("network.port", 11451)
        );

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            if (line.equals("exit")) {
                backendClient.disconnect();
                client.close();
                break;
            }
        }
    }
}
