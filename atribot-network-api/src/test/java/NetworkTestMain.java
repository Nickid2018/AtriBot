import io.github.nickid2018.atribot.network.BackendClient;
import io.github.nickid2018.atribot.network.BackendServer;
import lombok.SneakyThrows;

import java.net.InetAddress;
import java.util.Scanner;

public class NetworkTestMain {

    @SneakyThrows
    public static void main(String[] args) {
        BackendServer server = new BackendServer(() -> new TestListener("Server"));
        server.addPacket(TestPacket.class, TestPacket::new, true, true);
        server.setShouldEncrypt(true);
        server.start(11451);

        BackendClient client = new BackendClient(new TestListener("Client"));
        client.addPacket(TestPacket.class, TestPacket::new, true, true);
        client.connect(InetAddress.getLocalHost(), 11451);

        BackendClient client2 = new BackendClient(new TestListener("Client2"));
//        client2.addPacket(TestPacket.class, TestPacket::new);
        client2.connect(InetAddress.getLocalHost(), 11451);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String line = scanner.nextLine();
            if (line.equals("exit")) {
                client.disconnect();
                server.stop();
                break;
            }
            if (line.startsWith("send ")) {
                String[] split = line.split(" ", 2);
                TestPacket packet = new TestPacket(split[1]);
                client.sendPacket(packet);
                server.broadcastPacket(packet);
            }
        }
    }
}
