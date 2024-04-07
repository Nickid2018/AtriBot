package io.github.nickid2018.atribot.network.packet;

public interface Packet {

    void serializeToStream(PacketBuffer buffer) throws Exception;

    void deserializeFromStream(PacketBuffer buffer) throws Exception;
}
