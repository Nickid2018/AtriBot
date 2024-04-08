package io.github.nickid2018.atribot.network.message;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.nickid2018.atribot.network.connection.Connection;
import io.github.nickid2018.atribot.network.packet.Packet;
import io.github.nickid2018.atribot.network.packet.backend.StopTransactionPacket;
import io.github.nickid2018.atribot.network.packet.backend.TransactionPacket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class TransactionQueue {

    @Getter
    private final Connection connection;
    private final Map<String, Consumer<? extends TransactionPacket<?>>> transactionMap = new HashMap<>();
    private final Map<Class<? extends TransactionPacket<?>>, Consumer<? extends TransactionPacket<?>>> transactionConsumerMap = new HashMap<>();
    private final ExecutorService transactionExecutor = Executors.newThreadPerTaskExecutor(
            new ThreadFactoryBuilder()
                    .setThreadFactory(Thread.ofVirtual().factory())
                    .setDaemon(true)
                    .setNameFormat("TransactionQueue-%d")
                    .build()
    );

    public <T extends TransactionPacket<?>> void registerTransactionConsumer(Class<T> clazz, Consumer<T> consumer) {
        transactionConsumerMap.put(clazz, consumer);
    }

    public <T extends TransactionPacket<F>, F extends TransactionPacket<T>> void startTransaction(T packet, Consumer<F> consumer) {
        if (!packet.isQuery())
            throw new IllegalArgumentException("Transaction start packet must be a query!");
        while (transactionMap.containsKey(packet.getTransactionId()))
            packet.setTransactionId(UUID.randomUUID().toString());
        transactionMap.put(packet.getTransactionId(), consumer);
        connection.sendPacket(packet);
    }

    @SuppressWarnings("unchecked")
    public boolean resolveTransaction(Packet packet) {
        if (packet instanceof TransactionPacket<?> transactionPacket) {
            if (transactionPacket.isQuery()) {
                if (transactionConsumerMap.containsKey(transactionPacket.getClass())) {
                    Consumer<TransactionPacket<?>> consumer = (Consumer<TransactionPacket<?>>) transactionConsumerMap.get(
                            transactionPacket.getClass()
                    );
                    transactionExecutor.execute(() -> consumer.accept(transactionPacket));
                } else
                    return false;
            } else {
                String id = transactionPacket.getTransactionId();
                if (transactionMap.containsKey(id)) {
                    Consumer<TransactionPacket<?>> consumer = (Consumer<TransactionPacket<?>>) transactionMap.remove(id);
                    transactionExecutor.execute(() -> consumer.accept(transactionPacket));
                } else
                    log.warn("Transaction {} not found!", id);
            }
            return true;
        } else if (packet instanceof StopTransactionPacket stopTransactionPacket) {
            if (transactionMap.containsKey(stopTransactionPacket.getTransactionId())) {
                transactionMap.remove(stopTransactionPacket.getTransactionId());
                return true;
            }
        }
        return false;
    }
}
