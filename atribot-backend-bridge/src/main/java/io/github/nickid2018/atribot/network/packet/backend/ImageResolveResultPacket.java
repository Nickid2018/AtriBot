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
public class ImageResolveResultPacket extends TransactionPacket<ImageResolveStartPacket> {

    private Set<ImageMessage> imageMessage = new HashSet<>();

    public boolean isQuery() {
        return false;
    }

    @Override
    public void serializeToStream(PacketBuffer buffer) throws Exception {
        super.serializeToStream(buffer);
        buffer.writeVarInt(imageMessage.size());
        for (ImageMessage message : imageMessage)
            message.serializeToStream(buffer);
    }

    @Override
    public void deserializeFromStream(PacketBuffer buffer) throws Exception {
        super.deserializeFromStream(buffer);
        int size = buffer.readVarInt();
        for (int i = 0; i < size; i++) {
            ImageMessage message = new ImageMessage();
            message.deserializeFromStream(buffer);
            imageMessage.add(message);
        }
    }
}
