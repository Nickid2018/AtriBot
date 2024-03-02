import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TestPacket implements Packet {

    private String testString;

    @Override
    public void serializeToStream(PacketBuffer buffer) {
        buffer.writeString(testString);
    }

    @Override
    public void deserializeFromStream(PacketBuffer buffer) {
        testString = buffer.readString();
    }
}
