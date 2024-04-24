package io.github.nickid2018.atribot.plugins.calc;

import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.message.CommandCommunicateData;
import io.github.nickid2018.atribot.network.message.MessageChain;
import io.github.nickid2018.atribot.util.FunctionUtil;
import io.github.nickid2018.smcl.Statement;
import io.github.nickid2018.smcl.StatementParseException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@AllArgsConstructor
public class CalcReceiver implements CommunicateReceiver {

    private final CalcPlugin plugin;
    private static final Set<String> KEY = Set.of("atribot.message.command");

    @Override
    public <T, D> CompletableFuture<T> communicate(String communicateKey, D data) {
        CommandCommunicateData commandData = (CommandCommunicateData) data;
        if (commandData.commandHead.equals("calc")) {
            String statementStr = String.join(" ", commandData.commandArgs);
            Instant now = Instant.now();
            CompletableFuture
                .supplyAsync(FunctionUtil.noException(
                    () -> plugin.getContext().parse(statementStr)
                ), plugin.getExecutorService())
                .thenApply(FunctionUtil.<Statement, Double>noException(
                    statement -> statement.calculate(plugin.getDefaultVariables()).toStdNumber()
                ))
                .orTimeout(5, TimeUnit.SECONDS)
                .thenAccept(number -> commandData.messageManager.sendMessage(
                    commandData.backendID,
                    commandData.targetData,
                    MessageChain.text(
                        statementStr + " = " + number + "\n" +
                            "（耗时 " + (Instant.now().toEpochMilli() - now.toEpochMilli()) + "ms）"
                    )
                ))
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException) {
                        commandData.messageManager.sendMessage(
                            commandData.backendID,
                            commandData.targetData,
                            MessageChain.text("Calc：计算超时（5s）")
                        );
                    } else if (throwable.getCause() instanceof ArithmeticException) {
                        commandData.messageManager.sendMessage(
                            commandData.backendID,
                            commandData.targetData,
                            MessageChain.text("Calc：计算错误\n" + throwable.getCause().getMessage())
                        );
                    } else if (throwable.getCause() instanceof StatementParseException) {
                        commandData.messageManager.sendMessage(
                            commandData.backendID,
                            commandData.targetData,
                            MessageChain.text("Calc：表达式错误\n" + throwable.getCause().getMessage())
                        );
                    } else {
                        commandData.messageManager.sendMessage(
                            commandData.backendID,
                            commandData.targetData,
                            MessageChain.text("Calc：发生了未知的错误，错误报告如下\n" + throwable.getMessage())
                        );
                        log.error("Unexpected error in CalcReceiver", throwable);
                    }
                    return null;
                });
        }
        return null;
    }

    @Override
    public Set<String> availableCommunicateKeys() {
        return KEY;
    }
}
