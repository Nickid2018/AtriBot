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
