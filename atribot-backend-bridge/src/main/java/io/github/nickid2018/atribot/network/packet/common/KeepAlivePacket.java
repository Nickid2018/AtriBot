package io.github.nickid2018.atribot.network.packet.common;

import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
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
