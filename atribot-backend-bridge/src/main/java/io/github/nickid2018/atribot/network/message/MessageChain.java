package io.github.nickid2018.atribot.network.message;

import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class MessageChain implements Message {

    private final List<Message> messages = new ArrayList<>();

    public MessageChain next(Message message) {
        if (message instanceof MessageChain chain)
            messages.addAll(chain.messages);
        else
            messages.add(message);
        return this;
    }

    public MessageChain insert(int index, Message message) {
        if (message instanceof MessageChain chain)
            messages.addAll(index, chain.messages);
        else
            messages.add(index, message);
        return this;
    }

    public MessageChain flatten() {
        List<Message> newMessages = new ArrayList<>();
        for (Message message : messages) {
            if (message instanceof MessageChain chain)
                newMessages.addAll(chain.messages);
            else
                newMessages.add(message);
        }
        messages.clear();
        messages.addAll(newMessages);
        return this;
    }

    public void forEachMessage(Consumer<Message> consumer) {
        messages.forEach(consumer);
    }

    @Override
    public void serializeToStream(PacketBuffer buffer) throws Exception {
        buffer.writeVarInt(messages.size());
        for (Message message : messages) {
            buffer.writeString(Message.messageId(message));
            message.serializeToStream(buffer);
        }
    }

    @Override
    public void deserializeFromStream(PacketBuffer buffer) throws Exception {
        int size = buffer.readVarInt();
        for (int i = 0; i < size; i++) {
            String id = buffer.readString();
            Message message = Message.createMessage(id);
            message.deserializeFromStream(buffer);
            messages.add(message);
        }
    }

    public static MessageChain text(String text) {
        return new MessageChain().next(new TextMessage(text, false));
    }
}
