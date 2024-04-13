package io.github.nickid2018.atribot.network.packet.common;

import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.PacketBuffer;

public class ConnectionSuccessPacket implements Packet {

    @Override
    public void serializeToStream(PacketBuffer buffer) {
    }

    @Override
    public void deserializeFromStream(PacketBuffer buffer) {
    }

    @Override
    public int hashCode() {
        return 1145141919;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ConnectionSuccessPacket;
    }
}
