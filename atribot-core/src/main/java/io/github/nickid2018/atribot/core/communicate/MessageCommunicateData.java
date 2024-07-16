package io.github.nickid2018.atribot.core.communicate;

import io.github.nickid2018.atribot.core.message.MessageManager;
import io.github.nickid2018.atribot.network.message.MessageChain;
import io.github.nickid2018.atribot.network.message.TargetData;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MessageCommunicateData extends DiscardableCommunicateData {

    public String backendID;
    public MessageChain messageChain;
    public TargetData targetData;
    public MessageManager messageManager;
}
