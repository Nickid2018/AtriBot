package io.github.nickid2018.atribot.network.packet.backend;

import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SendReactionPacket implements Packet {

    private String messageID;
    private String reaction;

    @Override
    public void serializeToStream(PacketBuffer buffer) throws Exception {
        buffer.writeString(messageID);
        buffer.writeString(reaction);
    }

    @Override
    public void deserializeFromStream(PacketBuffer buffer) throws Exception {
        messageID = buffer.readString();
        reaction = buffer.readString();
    }
}
