package io.github.nickid2018.atribot.network.message;

import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
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

    public static String concatText(MessageChain chain) {
        StringBuilder builder = new StringBuilder();
        chain.forEachMessage(message -> {
            if (message instanceof TextMessage textMessage)
                builder.append(textMessage.text);
        });
        return builder.toString();
    }
}
