package io.github.nickid2018.atribot.network.message;

import io.github.nickid2018.atribot.network.packet.PacketBuffer;

public class UnsupportedMessage implements Message {

    public static final UnsupportedMessage INSTANCE = new UnsupportedMessage();

    @Override
    public void serializeToStream(PacketBuffer buffer) throws Exception {
    }

    @Override
    public void deserializeFromStream(PacketBuffer buffer) throws Exception {
    }
}
