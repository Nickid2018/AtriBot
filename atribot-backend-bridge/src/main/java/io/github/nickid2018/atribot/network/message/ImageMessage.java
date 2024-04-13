package io.github.nickid2018.atribot.network.message;

import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import lombok.*;

import java.net.URI;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ImageMessage implements Message {

    private String imgKey;
    private URI resolved;

    @Override
    public void serializeToStream(PacketBuffer buffer) throws Exception {
        buffer.writeString(imgKey);
        buffer.writeBoolean(resolved != null);
        if (resolved != null)
            buffer.writeString(resolved.toString());
    }

    @Override
    public void deserializeFromStream(PacketBuffer buffer) throws Exception {
        imgKey = buffer.readString();
        if (buffer.readBoolean())
            resolved = new URI(buffer.readString());
    }
}
