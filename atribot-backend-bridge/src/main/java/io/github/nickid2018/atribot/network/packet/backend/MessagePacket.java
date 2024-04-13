package io.github.nickid2018.atribot.network.packet.backend;

import io.github.nickid2018.atribot.network.message.MessageChain;
import io.github.nickid2018.atribot.network.message.TargetData;
import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MessagePacket implements Packet {

    private TargetData targetData;
    private MessageChain messageChain;

    @Override
    public void serializeToStream(PacketBuffer buffer) throws Exception {
        targetData.serializeToStream(buffer);
        messageChain.serializeToStream(buffer);
    }

    @Override
    public void deserializeFromStream(PacketBuffer buffer) throws Exception {
        targetData = new TargetData();
        targetData.deserializeFromStream(buffer);
        messageChain = new MessageChain();
        messageChain.deserializeFromStream(buffer);
    }
}
