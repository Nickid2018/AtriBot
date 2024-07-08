package io.github.nickid2018.atribot.plugins.qrcode;

import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.plugin.AbstractAtriBotPlugin;
import io.github.nickid2018.atribot.core.plugin.PluginInfo;

public class QRCodePlugin extends AbstractAtriBotPlugin {

    private final QRCodeReceiver receiver = new QRCodeReceiver(this);

    @Override
    public PluginInfo getPluginInfo() {
        return new PluginInfo(
            "atribot-plugin-qrcode",
            "QRCode",
            "1.0",
            "Nickid2018",
            "A plugin to generate/scan QR code."
        );
    }

    @Override
    public CommunicateReceiver getCommunicateReceiver() {
        return receiver;
    }
}
