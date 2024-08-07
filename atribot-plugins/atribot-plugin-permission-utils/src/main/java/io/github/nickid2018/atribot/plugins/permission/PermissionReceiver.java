package io.github.nickid2018.atribot.plugins.permission;

import io.github.nickid2018.atribot.core.communicate.Communicate;
import io.github.nickid2018.atribot.core.communicate.CommunicateFilter;
import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.message.CommandCommunicateData;
import io.github.nickid2018.atribot.core.message.MessageManager;
import io.github.nickid2018.atribot.core.message.PermissionLevel;
import io.github.nickid2018.atribot.core.message.PermissionManager;
import io.github.nickid2018.atribot.network.message.AtMessage;
import io.github.nickid2018.atribot.network.message.MessageChain;
import io.github.nickid2018.atribot.network.message.TargetData;
import io.github.nickid2018.atribot.util.FunctionUtil;
import lombok.AllArgsConstructor;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@AllArgsConstructor
public class PermissionReceiver implements CommunicateReceiver {

    private PermissionPlugin plugin;

    private String[] getIDs(MessageChain chain) {
        return chain
            .getMessages()
            .stream()
            .filter(AtMessage.class::isInstance)
            .map(AtMessage.class::cast)
            .map(AtMessage::getTargetData)
            .filter(TargetData::isUserSpecified)
            .map(TargetData::getTargetUser)
            .collect(Collectors.toSet())
            .toArray(String[]::new);
    }

    @Communicate("command.normal")
    @CommunicateFilter(key = "permission", value = "SUPER_USER")
    @CommunicateFilter(key = "name", value = "admin")
    @CommunicateFilter(key = "name", value = "mod")
    public void doAdmin(CommandCommunicateData commandData) {
        String[] ids = getIDs(commandData.messageChain);
        if (ids.length == 0)
            return;
        doPermissionState(
            ids,
            commandData.commandArgs.length > 0 ? commandData.commandArgs[0] : null,
            commandData.backendID,
            commandData.messageManager,
            commandData.targetData,
            commandData.messageManager.getPermissionManager(),
            PermissionLevel.ADMIN
        );
    }

    @Communicate("command.normal")
    @CommunicateFilter(key = "permission", value = "ADMIN")
    @CommunicateFilter(key = "name", value = "semiban")
    public void doSemiBan(CommandCommunicateData commandData) {
        String[] ids = getIDs(commandData.messageChain);
        if (ids.length == 0)
            return;
        doPermissionState(
            ids,
            commandData.commandArgs.length > 0 ? commandData.commandArgs[0] : null,
            commandData.backendID,
            commandData.messageManager,
            commandData.targetData,
            commandData.messageManager.getPermissionManager(),
            PermissionLevel.SEMI_BANNED
        );
    }

    @Communicate("command.normal")
    @CommunicateFilter(key = "permission", value = "ADMIN")
    @CommunicateFilter(key = "name", value = "ban")
    public void doBan(CommandCommunicateData commandData) {
        String[] ids = getIDs(commandData.messageChain);
        if (ids.length == 0)
            return;
        doPermissionState(
            ids,
            commandData.commandArgs.length > 0 ? commandData.commandArgs[0] : null,
            commandData.backendID,
            commandData.messageManager,
            commandData.targetData,
            commandData.messageManager.getPermissionManager(),
            PermissionLevel.BANNED
        );
    }

    @Communicate("command.normal")
    @CommunicateFilter(key = "permission", value = "ADMIN")
    @CommunicateFilter(key = "name", value = "beuser")
    public void doUser(CommandCommunicateData commandData) {
        String[] ids = getIDs(commandData.messageChain);
        if (ids.length == 0)
            return;
        runAndCatch(() -> {
            for (String id : ids)
                commandData.messageManager.getPermissionManager().clearPermission(id);
            commandData.messageManager.sendMessage(
                commandData.backendID,
                commandData.targetData,
                MessageChain.text("Permission-Utils：权限设置成功！")
            );
        }, commandData.backendID, commandData.messageManager, commandData.targetData);
    }

    private static final Pattern TIME_PATTERN = Pattern.compile("\\d+[smhdy]");
    private static final Pattern MATCH_PATTERN = Pattern.compile("(\\d+[smhdy])+");

    private long formatTime(String time) {
        if (!MATCH_PATTERN.matcher(time).matches())
            throw new IllegalArgumentException("无效的时间格式！");
        Matcher matcher = TIME_PATTERN.matcher(time);
        long result = 0;
        while (matcher.find()) {
            String group = matcher.group();
            int length = group.length();
            long number = Long.parseLong(group.substring(0, length - 1));
            switch (group.charAt(length - 1)) {
                case 's' -> result += number;
                case 'm' -> result += number * 60;
                case 'h' -> result += number * 3600;
                case 'd' -> result += number * 86400;
                case 'y' -> result += number * 31536000;
            }
        }
        return result * 1000;
    }

    private void doPermissionState(String[] users, String argOuter, String backend, MessageManager messageManager, TargetData target, PermissionManager manager, PermissionLevel level) {
        runAndCatch(() -> {
            String arg = argOuter;
            if (arg == null) {
                for (String user : users)
                    manager.setPermission(user, level);
            } else {
                int mode = 0;
                if (arg.startsWith("+")) {
                    mode = 1;
                    arg = arg.substring(1);
                } else if (arg.startsWith("-")) {
                    mode = 2;
                    arg = arg.substring(1);
                }
                long time = formatTime(arg);

                for (String user : users) {
                    if (mode == 1) {
                        PermissionLevel nowLevel = manager.getPermissionLevel(user);
                        if (nowLevel != level)
                            throw new IllegalArgumentException("权限不匹配！");
                        long nowExpire = manager.getPermissionTimestamp(user);
                        if (nowExpire != Long.MAX_VALUE)
                            manager.setPermission(user, level, time + nowExpire);
                    } else if (mode == 2) {
                        PermissionLevel nowLevel = manager.getPermissionLevel(user);
                        if (nowLevel != level)
                            throw new IllegalArgumentException("权限不匹配！");
                        long nowExpire = manager.getPermissionTimestamp(user);
                        if (nowExpire != Long.MAX_VALUE)
                            manager.setPermission(user, level, nowExpire - time);
                    } else {
                        manager.setPermission(user, level, System.currentTimeMillis() + time);
                    }
                }
            }
            messageManager.sendMessage(
                backend,
                target,
                MessageChain.text("Permission-Utils：权限设置成功！")
            );
        }, backend, messageManager, target);
    }

    private void runAndCatch(FunctionUtil.RunnableWithException<?> runnable, String backend, MessageManager messageManager, TargetData target) {
        plugin.getExecutorService().execute(() -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                messageManager.sendMessage(
                    backend,
                    target,
                    MessageChain.text("Permission-Utils：权限设置失败：" + e.getMessage())
                );
            }
        });
    }
}
