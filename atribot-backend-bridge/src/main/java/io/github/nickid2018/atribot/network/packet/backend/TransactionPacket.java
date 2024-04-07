package io.github.nickid2018.atribot.network.packet.backend;

import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("unused")
public abstract class TransactionPacket<P extends TransactionPacket<?>> implements Packet {

    private String transactionId = UUID.randomUUID().toString();

    public abstract boolean isQuery();

    public void serializeToStream(PacketBuffer buffer) throws Exception {
        buffer.writeString(transactionId);
    }

    public void deserializeFromStream(PacketBuffer buffer) throws Exception {
        transactionId = buffer.readString();
    }
}
