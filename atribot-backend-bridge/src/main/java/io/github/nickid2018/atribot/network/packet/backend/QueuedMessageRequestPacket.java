package io.github.nickid2018.atribot.network.packet.backend;

import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class QueuedMessageRequestPacket implements Packet {

    public static final QueuedMessageRequestPacket INSTANCE = new QueuedMessageRequestPacket();

    @Override
    public void serializeToStream(PacketBuffer buffer) throws Exception {
    }

    @Override
    public void deserializeFromStream(PacketBuffer buffer) throws Exception {
    }
}
