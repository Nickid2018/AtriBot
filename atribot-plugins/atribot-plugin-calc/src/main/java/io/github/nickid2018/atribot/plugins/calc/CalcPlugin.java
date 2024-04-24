package io.github.nickid2018.atribot.plugins.calc;

import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.plugin.AbstractAtriBotPlugin;
import io.github.nickid2018.atribot.core.plugin.PluginInfo;
import io.github.nickid2018.smcl.SMCLContext;
import io.github.nickid2018.smcl.SMCLSettings;
import io.github.nickid2018.smcl.VariableValueList;
import io.github.nickid2018.smcl.statements.NumberStatement;
import lombok.Getter;

public class CalcPlugin extends AbstractAtriBotPlugin {

    @Getter
    private SMCLContext context;
    @Getter
    private VariableValueList defaultVariables;
    private final CalcReceiver receiver = new CalcReceiver(this);

    @Override
    public PluginInfo getPluginInfo() {
        return new PluginInfo(
            "atribot-plugin-calc",
            "calc",
            "1.0",
            "Nickid2018",
            "A plugin to calculate the expression"
        );
    }

    @Override
    public void onPluginLoad() throws Exception {
        context = new SMCLContext(new SMCLSettings());
        context.init();
        context.globalvars.registerVariable("k");
        context.register.registerConstant("phi",
                                          new NumberStatement(context, context.numberProvider.fromStdNumber(Math.sqrt(5) / 2 + 0.5)));
        context.register.registerFunction("rand", new StandardRandomFunctionParser(false));
        context.register.registerFunction("randint", new StandardRandomFunctionParser(true));
        context.register.registerFunction("sum", new SumFunctionParser());
        defaultVariables = new VariableValueList(context);
    }

    @Override
    public CommunicateReceiver getCommunicateReceiver() {
        return receiver;
    }
}
