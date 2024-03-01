package io.github.nickid2018.atribot.network.connection;

import io.github.nickid2018.atribot.network.packet.Packet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class PacketRegistry {

    private final Int2ObjectMap<Supplier<? extends Packet>> packetCreationRegistry = new Int2ObjectOpenHashMap<>();
    private final Object2IntMap<Class<? extends Packet>> packetSerializeRegistry = new Object2IntOpenHashMap<>();
    private final IntSet serverSidePackets = new IntOpenHashSet();
    private final IntSet clientSidePackets = new IntOpenHashSet();
    private final AtomicInteger idCounter = new AtomicInteger(0);

    public <T extends Packet> int registerPacket(Class<T> packetClass, Supplier<T> supplier, boolean serverSide, boolean clientSide) {
        int id = idCounter.getAndIncrement();
        packetCreationRegistry.put(id, supplier);
        packetSerializeRegistry.put(packetClass, id);
        if (serverSide)
            serverSidePackets.add(id);
        if (clientSide)
            clientSidePackets.add(id);
        return id;
    }

    public int getPacketId(Packet packet) {
        return packetSerializeRegistry.getOrDefault(packet.getClass(), -1);
    }

    public Packet createPacket(int id, boolean serverSide) {
        if (!packetCreationRegistry.containsKey(id))
            throw new IllegalArgumentException("Unknown packet id: " + id);
        if (serverSide && !serverSidePackets.contains(id))
            throw new IllegalArgumentException(
                    "Packet id %d is not server side packet"
            );
        if (!serverSide && !clientSidePackets.contains(id))
            throw new IllegalArgumentException(
                    "Packet id %d is not client side packet"
            );
        return packetCreationRegistry.get(id).get();
    }
}
