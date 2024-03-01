package io.github.nickid2018.atribot.network.connection;

import io.github.nickid2018.atribot.network.packet.Packet;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public record PacketHolder(Packet packet, GenericFutureListener<? extends Future<? super Void>> listener) {
}
