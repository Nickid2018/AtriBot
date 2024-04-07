package io.github.nickid2018.atribot.network.message;

import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TextMessage implements Message {

    private String text;
    private boolean markdown;

    @Override
    public void serializeToStream(PacketBuffer buffer) throws Exception {
        buffer.writeString(text);
        buffer.writeBoolean(markdown);
    }

    @Override
    public void deserializeFromStream(PacketBuffer buffer) throws Exception {
        text = buffer.readString();
        markdown = buffer.readBoolean();
    }
}
