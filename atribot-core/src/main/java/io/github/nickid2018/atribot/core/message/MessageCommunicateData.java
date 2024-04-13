package io.github.nickid2018.atribot.core.message;

import io.github.nickid2018.atribot.network.message.MessageChain;
import io.github.nickid2018.atribot.network.message.TargetData;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MessageCommunicateData {

    public String backendID;
    public MessageChain messageChain;
    public TargetData targetData;
    public MessageManager messageManager;
}
