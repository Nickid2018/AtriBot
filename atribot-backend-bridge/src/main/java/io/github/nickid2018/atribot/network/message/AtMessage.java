package io.github.nickid2018.atribot.network.message;

import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class AtMessage implements Message {

    private TargetData targetData;

    @Override
    public void serializeToStream(PacketBuffer buffer) throws Exception {
        targetData.serializeToStream(buffer);
    }

    @Override
    public void deserializeFromStream(PacketBuffer buffer) throws Exception {
        targetData = new TargetData();
        targetData.deserializeFromStream(buffer);
    }
}
