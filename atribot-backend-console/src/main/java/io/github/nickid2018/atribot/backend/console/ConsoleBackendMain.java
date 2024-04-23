package io.github.nickid2018.atribot.backend.console;

import io.github.nickid2018.atribot.network.BackendClient;
import io.github.nickid2018.atribot.network.PacketRegister;
import io.github.nickid2018.atribot.network.message.MessageChain;
import io.github.nickid2018.atribot.network.message.TargetData;
import io.github.nickid2018.atribot.network.message.TextMessage;
import io.github.nickid2018.atribot.network.message.TransactionQueue;
import io.github.nickid2018.atribot.network.packet.backend.MessagePacket;
import io.github.nickid2018.atribot.util.ClassPathDependencyResolver;
import io.github.nickid2018.atribot.util.Configuration;
import io.github.nickid2018.atribot.util.LogUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.Scanner;

@Slf4j
public class ConsoleBackendMain {

    @SneakyThrows
    public static void main(String[] args) {
        if (ClassPathDependencyResolver.inProductionEnvironment(ConsoleBackendMain.class))
            ClassPathDependencyResolver.resolveCoreDependencies();

        LogUtil.redirectJULToSLF4J();
        Configuration.init();

        ConsoleBackendListener listener = new ConsoleBackendListener();
        BackendClient client = new BackendClient(listener);
        listener.setTransactionQueue(new TransactionQueue(client::getConnection));
        PacketRegister.registerBackendPackets(client);
        client.setAutoReconnect(true);
        client.connect(
                InetAddress.getByName(Configuration.getStringOrElse("network.host", "localhost")),
                Configuration.getIntOrElse("network.port", 11451)
        );

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            if (line.equals("exit")) {
                client.disconnect();
                break;
            } else {
                client.sendPacket(new MessagePacket(
                        new TargetData(null, "console"),
                        new MessageChain().next(new TextMessage(line, false))
                ));
            }
        }
    }
}
