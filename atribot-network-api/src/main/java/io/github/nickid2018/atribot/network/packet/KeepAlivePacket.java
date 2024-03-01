package io.github.nickid2018.atribot.network.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KeepAlivePacket implements Packet {

    private long time;

    public static KeepAlivePacket createNow() {
        return new KeepAlivePacket(System.currentTimeMillis());
    }

    @Override
    public void serializeToStream(PacketBuffer buffer) {
        buffer.writeLong(time);
    }

    @Override
    public void deserializeFromStream(PacketBuffer buffer) {
        time = buffer.readLong();
    }
}
