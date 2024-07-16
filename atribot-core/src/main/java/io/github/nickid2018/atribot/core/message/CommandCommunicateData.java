package io.github.nickid2018.atribot.core.message;

import io.github.nickid2018.atribot.core.communicate.MessageCommunicateData;
import io.github.nickid2018.atribot.network.message.MessageChain;
import io.github.nickid2018.atribot.network.message.TargetData;

public class CommandCommunicateData extends MessageCommunicateData {

    public String commandHead;
    public String[] commandArgs;

    public CommandCommunicateData(
        String backendID, MessageChain messageChain, TargetData targetData,
        MessageManager messageManager, String commandHead, String[] commandArgs
    ) {
        super(backendID, messageChain, targetData, messageManager);
        this.commandHead = commandHead;
        this.commandArgs = commandArgs;
    }
}
