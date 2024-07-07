package io.github.nickid2018.atribot.network.message;

import io.github.nickid2018.atribot.network.packet.PacketBuffer;

import java.io.Serializable;

public interface Message extends Serializable {

    void serializeToStream(PacketBuffer buffer) throws Exception;

    void deserializeFromStream(PacketBuffer buffer) throws Exception;

    static String messageId(Message message) {
        return switch (message) {
            case MessageChain ignored -> "sub_chain";
            case TextMessage ignored -> "text";
            case ImageMessage ignored -> "image";
            case AtMessage ignored -> "at";
            case MsgIDMessage ignored -> "msg_id";
            case UnsupportedMessage ignored -> "unsupported";
            default -> throw new IllegalArgumentException("Unknown message: " + message);
        };
    }

    static Message createMessage(String id) {
        return switch (id) {
            case "sub_chain" -> new MessageChain();
            case "text" -> new TextMessage();
            case "image" -> new ImageMessage();
            case "at" -> new AtMessage();
            case "msg_id" -> new MsgIDMessage();
            case "unsupported" -> UnsupportedMessage.INSTANCE;
            default -> throw new IllegalArgumentException("Unknown message id: " + id);
        };
    }
}
