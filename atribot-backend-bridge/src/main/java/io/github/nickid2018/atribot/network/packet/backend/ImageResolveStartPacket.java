package io.github.nickid2018.atribot.network.packet.backend;


import io.github.nickid2018.atribot.network.message.ImageMessage;
import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImageResolveStartPacket extends TransactionPacket<ImageResolveResultPacket> {

    private Set<String> imageMessageKeys = new HashSet<>();

    @Override
    public boolean isQuery() {
        return true;
    }

    @Override
    public void serializeToStream(PacketBuffer buffer) throws Exception {
        super.serializeToStream(buffer);
        buffer.writeVarInt(imageMessageKeys.size());
        for (String key : imageMessageKeys)
            buffer.writeString(key);
    }

    @Override
    public void deserializeFromStream(PacketBuffer buffer) throws Exception {
        super.deserializeFromStream(buffer);
        int size = buffer.readVarInt();
        for (int i = 0; i < size; i++)
            imageMessageKeys.add(buffer.readString());
    }
}
