import io.github.nickid2018.atribot.network.connection.Connection;
import io.github.nickid2018.atribot.network.listener.NetworkListener;
import io.github.nickid2018.atribot.network.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class TestListener implements NetworkListener {

    private String name;

    @Override
    public void connectionOpened(Connection connection) {
        log.info("Connection opened: " + name);
    }

    @Override
    public void receivePacket(Connection connection, Packet msg) {
        if (msg instanceof TestPacket test) {
            log.info("Received packet (" + name + "): " + test.getTestString());
        }
    }

    @Override
    public void connectionClosed(Connection connection) {
        log.info("Connection closed: " + name);
    }

    @Override
    public void exceptionCaught(Connection connection, Throwable cause) {
        log.error("Exception caught: " + name, cause);
    }

    @Override
    public void fatalError(Connection connection, Throwable cause) {
        log.error("Fatal error: " + name, cause);
    }
}
