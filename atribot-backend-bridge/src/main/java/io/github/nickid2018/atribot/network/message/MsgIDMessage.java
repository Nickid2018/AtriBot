package io.github.nickid2018.atribot.network.message;

import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class MsgIDMessage implements Message {

    private String msgID;

    @Override
    public void serializeToStream(PacketBuffer buffer) throws Exception {
        buffer.writeString(msgID);
    }

    @Override
    public void deserializeFromStream(PacketBuffer buffer) throws Exception {
        msgID = buffer.readString();
    }
}
