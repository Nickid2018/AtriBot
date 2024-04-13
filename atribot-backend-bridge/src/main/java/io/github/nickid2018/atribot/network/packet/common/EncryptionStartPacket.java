package io.github.nickid2018.atribot.network.packet.common;

import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EncryptionStartPacket implements Packet {

    private byte[] publicKey;
    private byte[] challenge;

    public void serializeToStream(PacketBuffer buffer) {
        buffer.writeByteArray(publicKey);
        buffer.writeByteArray(challenge);
    }

    public void deserializeFromStream(PacketBuffer buffer) {
        publicKey = buffer.readByteArray();
        challenge = buffer.readByteArray();
    }
}
