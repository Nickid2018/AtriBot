package io.github.nickid2018.atribot.plugins.command;

import com.google.common.base.Predicates;
import io.github.nickid2018.atribot.core.communicate.*;
import io.github.nickid2018.atribot.core.message.CommandCommunicateData;
import io.github.nickid2018.atribot.core.message.PermissionLevel;
import io.github.nickid2018.atribot.core.plugin.AtriBotPlugin;
import io.github.nickid2018.atribot.core.plugin.PluginInfo;
import io.github.nickid2018.atribot.network.message.TextMessage;

import java.util.regex.Pattern;

public class CommandPlugin implements AtriBotPlugin, CommunicateReceiver {

    @Override
    public PluginInfo getPluginInfo() {
        return new PluginInfo(
            "atribot-plugin-command",
            "Command",
            "1.0",
            "Nickid2018",
            "A plugin for command"
        );
    }

    @Override
    public CommunicateReceiver getCommunicateReceiver() {
        return this;
    }

    @Override
    public void onPluginPreload() {
    }

    @Override
    public void onPluginLoad() {
    }

    @Override
    public void onPluginUnload() {
    }

    @Communicate(value = "atribot.message.command", priority = -1)
    public void onCommand(CommandCommunicateData command) {
        Communication.communicate("command.normal", Predicates.alwaysTrue(), communicateFilters -> {
            PermissionLevel need = PermissionLevel.USER;
            boolean hasMatch = false;
            boolean ignoreCase = false;
            for (CommunicateFilter filter : communicateFilters) {
                String key = filter.key();
                String value = filter.value();
                switch (key) {
                    case "permission" -> need = PermissionLevel.valueOf(value);
                    case "case" -> ignoreCase = Boolean.parseBoolean(value);
                    case "name" -> {
                        if (ignoreCase ? command.commandHead.equalsIgnoreCase(value) : command.commandHead.equals(value))
                            hasMatch = true;
                    }
                }
            }
            return hasMatch && command.messageManager.getPermissionManager().hasPermission(
                command.targetData.getTargetUser(),
                need
            );
        }, command);
    }

    @Communicate(value = "atribot.message.normal", priority = -1)
    public void onRegex(MessageCommunicateData message) {
        Communication.communicate("command.regex", Predicates.alwaysTrue(), communicateFilters -> {
            boolean hasMatch = false;
            PermissionLevel need = PermissionLevel.USER;
            for (CommunicateFilter filter : communicateFilters) {
                String key = filter.key();
                String value = filter.value();
                switch (key) {
                    case "permission" -> need = PermissionLevel.valueOf(value);
                    case "regex" -> {
                        Pattern pattern = Pattern.compile(value);
                        if (pattern.matcher(TextMessage.concatText(message.messageChain)).find())
                            hasMatch = true;
                    }
                }
            }
            return hasMatch && message.messageManager.getPermissionManager().hasPermission(
                message.targetData.getTargetUser(),
                need
            );
        }, message);
    }
}
