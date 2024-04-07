package io.github.nickid2018.atribot.network.packet.backend;

import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BackendBasicInformationPacket implements Packet {

    public static final int PROTOCOL_VERSION = 0x1001;

    private String identifier;
    private String version;
    private Map<String, String> externalInformation = new HashMap<>();

    @Override
    public void serializeToStream(PacketBuffer buffer) {
        buffer.writeString(identifier);
        buffer.writeString(version);
        buffer.writeVarInt(PROTOCOL_VERSION);
        int size = externalInformation.size();
        buffer.writeVarInt(size);
        for (Map.Entry<String, String> entry : externalInformation.entrySet()) {
            buffer.writeString(entry.getKey());
            buffer.writeString(entry.getValue());
        }
    }

    @Override
    public void deserializeFromStream(PacketBuffer buffer) {
        identifier = buffer.readString();
        version = buffer.readString();
        int protocol = buffer.readVarInt();
        if (protocol != PROTOCOL_VERSION)
            throw new IllegalStateException("Protocol version mismatch! Expected " + PROTOCOL_VERSION + ", got " + protocol);
        int size = buffer.readVarInt();
        for (int i = 0; i < size; i++)
            externalInformation.put(buffer.readString(), buffer.readString());
    }
}
