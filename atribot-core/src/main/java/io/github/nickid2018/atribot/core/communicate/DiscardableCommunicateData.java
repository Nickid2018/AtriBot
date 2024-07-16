package io.github.nickid2018.atribot.core.communicate;

import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class DiscardableCommunicateData {

    @Setter
    private boolean canBeDiscarded = false;
    private boolean discarded = false;

    public void discard() {
        if (canBeDiscarded)
            discarded = true;
    }
}
