package io.github.nickid2018.atribot.network.message;

import io.github.nickid2018.atribot.network.packet.PacketBuffer;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class TargetData implements Serializable {

    private String targetGroup;
    private String targetUser;

    public void serializeToStream(PacketBuffer buffer) throws Exception {
        buffer.writeBoolean(targetGroup != null);
        if (targetGroup != null)
            buffer.writeString(targetGroup);
        buffer.writeBoolean(targetUser != null);
        if (targetUser != null)
            buffer.writeString(targetUser);
    }

    public void deserializeFromStream(PacketBuffer buffer) throws Exception {
        if (buffer.readBoolean())
            targetGroup = buffer.readString();
        if (buffer.readBoolean())
            targetUser = buffer.readString();
    }

    public boolean isGroupMessage() {
        return targetGroup != null;
    }

    public boolean isUserSpecified() {
        return targetUser != null;
    }

    public boolean isInvalid() {
        return targetGroup == null && targetUser == null;
    }
}
