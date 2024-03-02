package io.github.nickid2018.atribot.network.packet.common;

import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EncryptionProgressPacket implements Packet {

    private byte[] encryptedSecretKey;
    private byte[] encryptedChallenge;

    @Override
    public void deserializeFromStream(PacketBuffer buffer) {
        encryptedSecretKey = buffer.readByteArray();
        encryptedChallenge = buffer.readByteArray();
    }

    @Override
    public void serializeToStream(PacketBuffer buffer) {
        buffer.writeByteArray(encryptedSecretKey);
        buffer.writeByteArray(encryptedChallenge);
    }
}
