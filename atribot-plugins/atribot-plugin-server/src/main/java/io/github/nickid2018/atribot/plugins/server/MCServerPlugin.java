package io.github.nickid2018.atribot.plugins.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.net.httpserver.*;
import io.github.nickid2018.atribot.core.communicate.Communicate;
import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.core.communicate.Communication;
import io.github.nickid2018.atribot.core.plugin.AbstractAtriBotPlugin;
import io.github.nickid2018.atribot.core.plugin.PluginInfo;
import io.github.nickid2018.atribot.util.Configuration;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.concurrent.Executors;

@Slf4j
public class MCServerPlugin extends AbstractAtriBotPlugin implements CommunicateReceiver {

    private HttpServer httpServer;

    @Override
    public PluginInfo getPluginInfo() {
        return new PluginInfo(
            "atribot-plugin-server",
            "Atribot Server Provider",
            "1.0",
            "Nickid2018",
            "Provide Server Service"
        );
    }

    @Override
    public void onPluginPreload() throws Exception {
        super.onPluginPreload();
        int serverPort = Configuration.getIntOrElse("server.port", 8080);
        String keyStorePath = Configuration.getString("server.keystore.path");
        String keyStorePassword = Configuration.getString("server.keystore.password");
        if (keyStorePath != null && keyStorePassword != null) {
            httpServer = HttpsServer.create(new InetSocketAddress(serverPort), 0);
            KeyStore ks = KeyStore.getInstance("JKS");
            FileInputStream fis = new FileInputStream(keyStorePath);
            ks.load(fis, keyStorePassword.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, keyStorePassword.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            ((HttpsServer) httpServer).setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                    try {
                        SSLContext c = SSLContext.getDefault();
                        SSLEngine engine = c.createSSLEngine();
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        params.setProtocols(engine.getEnabledProtocols());
                        SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
                        params.setSSLParameters(defaultSSLParameters);
                    } catch (Exception ex) {
                        log.error(ex.getMessage(), ex);
                    }
                }
            });
        } else {
            httpServer = HttpServer.create(new InetSocketAddress(serverPort), 0);
        }
        httpServer.setExecutor(Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                .setDaemon(true)
                .setUncaughtExceptionHandler((_, t) -> log.error("Error in Server", t))
                .build()
        ));
        httpServer.start();
        log.info("Starting Server on port {}", serverPort);
    }

    @Override
    public void onPluginLoad() {
        if (httpServer == null)
            return;
        Communication.communicate("server.service.started");
    }

    @Override
    public void onPluginUnload() {
        if (httpServer == null)
            return;
        Communication.communicate("server.service.stopped");
        httpServer.stop(0);
    }

    @Communicate("server.register")
    public void addHandler(String path, HttpHandler handler) {
        if (httpServer == null)
            return;
        httpServer.createContext(path, handler);
        log.info("Handler added to path {}", path);
    }

    @Communicate("server.unregister")
    public void removeHandler(String path) {
        if (httpServer == null)
            return;
        httpServer.removeContext(path);
        log.info("Handler removed from path {}", path);
    }

    @Override
    public CommunicateReceiver getCommunicateReceiver() {
        return this;
    }
}
