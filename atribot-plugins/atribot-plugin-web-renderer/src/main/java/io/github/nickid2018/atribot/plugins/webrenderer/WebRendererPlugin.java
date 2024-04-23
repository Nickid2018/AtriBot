package io.github.nickid2018.atribot.plugins.webrenderer;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.plugin.AbstractAtriBotPlugin;
import io.github.nickid2018.atribot.core.plugin.PluginInfo;
import io.github.nickid2018.atribot.util.Configuration;
import lombok.Getter;
import lombok.SneakyThrows;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebRendererPlugin extends AbstractAtriBotPlugin {

    private final WebRendererReceiver receiver = new WebRendererReceiver(this);
    @Getter
    private ExecutorService rendererExecutor;

    @Override
    public PluginInfo getPluginInfo() {
        return new PluginInfo(
            "atribot-plugin-web-renderer",
            "Web Renderer",
            "1.0",
            "Nickid2018",
            "A plugin to render web pages"
        );
    }

    @Override
    public CommunicateReceiver getCommunicateReceiver() {
        return receiver;
    }

    @Override
    public void onPluginPreload() throws Exception {
        rendererExecutor = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder()
                .setNameFormat("WebRenderer-%d")
                .setDaemon(true)
                .build()
        );

        super.onPluginPreload();
    }

    @SneakyThrows
    public RemoteWebDriver createNewDriver() {
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");

        if (Configuration.hasKey("webrenderer.profile_root"))
            options.addArguments("--profile-root", Configuration.getString("webrenderer.profile_root"));

        if (Configuration.hasKey("webrenderer.proxy")) {
            String proxyProtocol = Configuration.getStringOrElse("web_renderer.proxy.protocol", "socks");
            String proxyHost = Configuration.getStringOrElse("web_renderer.proxy.host", "localhost");
            int proxyPort = Configuration.getIntOrElse("web_renderer.proxy.port", 7890);
            Proxy proxy = new Proxy();
            switch (proxyProtocol) {
                case "socks" -> proxy.setSocksProxy(proxyHost + ":" + proxyPort);
                case "http" -> proxy.setHttpProxy(proxyHost + ":" + proxyPort);
                case "ssl" -> proxy.setSslProxy(proxyHost + ":" + proxyPort);
            }
            options.setProxy(proxy);
        }

        return new RemoteWebDriver(
            URI.create(Configuration.getStringOrElse("webrenderer.url", "http://localhost:4444")).toURL(),
            options
        );
    }

    @Override
    public void onPluginUnload() throws Exception {
        super.onPluginUnload();
        rendererExecutor.shutdown();
    }
}
