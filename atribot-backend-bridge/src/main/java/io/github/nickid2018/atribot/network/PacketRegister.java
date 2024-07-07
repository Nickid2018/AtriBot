package io.github.nickid2018.atribot.network;

import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.backend.*;
import io.github.nickid2018.atribot.network.packet.common.ConnectionSuccessPacket;
import io.github.nickid2018.atribot.network.packet.common.EncryptionProgressPacket;
import io.github.nickid2018.atribot.network.packet.common.EncryptionStartPacket;
import io.github.nickid2018.atribot.network.packet.common.KeepAlivePacket;

import java.util.function.Supplier;

public interface PacketRegister {

    <T extends Packet> void addPacket(Class<T> packetClass, Supplier<T> supplier, boolean serverSide, boolean clientSide);

    static void registerBasicPackets(PacketRegister register) {
        register.addPacket(KeepAlivePacket.class, KeepAlivePacket::new, true, true);
        register.addPacket(EncryptionStartPacket.class, EncryptionStartPacket::new, true, false);
        register.addPacket(EncryptionProgressPacket.class, EncryptionProgressPacket::new, false, true);
        register.addPacket(ConnectionSuccessPacket.class, ConnectionSuccessPacket::new, true, false);
    }

    static void registerBackendPackets(PacketRegister register) {
        register.addPacket(BackendBasicInformationPacket.class, BackendBasicInformationPacket::new, false, true);
        register.addPacket(MessagePacket.class, MessagePacket::new, false, true);
        register.addPacket(SendMessagePacket.class, SendMessagePacket::new, true, false);
        register.addPacket(SendReactionPacket.class, SendReactionPacket::new, true, false);
        register.addPacket(MessageSentPacket.class, MessageSentPacket::new, false, true);
        register.addPacket(QueuedMessageRequestPacket.class, QueuedMessageRequestPacket::new, false, true);
        register.addPacket(StopTransactionPacket.class, StopTransactionPacket::new, true, true);
        register.addPacket(ImageResolveStartPacket.class, ImageResolveStartPacket::new, true, true);
        register.addPacket(ImageResolveResultPacket.class, ImageResolveResultPacket::new, true, true);
    }
}
