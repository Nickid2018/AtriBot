package io.github.nickid2018.atribot.network.packet.backend;

import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StopTransactionPacket implements Packet {

    private String transactionId = UUID.randomUUID().toString();

    public void serializeToStream(PacketBuffer buffer) throws Exception {
        buffer.writeString(transactionId);
    }

    public void deserializeFromStream(PacketBuffer buffer) throws Exception {
        transactionId = buffer.readString();
    }
}
