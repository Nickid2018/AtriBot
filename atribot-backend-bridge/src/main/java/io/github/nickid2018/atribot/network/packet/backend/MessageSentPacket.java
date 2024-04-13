package io.github.nickid2018.atribot.network.packet.backend;

import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MessageSentPacket implements Packet {
    private String uniqueID;

    @Override
    public void serializeToStream(PacketBuffer buffer) throws Exception {
        buffer.writeString(uniqueID);
    }

    @Override
    public void deserializeFromStream(PacketBuffer buffer) throws Exception {
        uniqueID = buffer.readString();
    }
}
