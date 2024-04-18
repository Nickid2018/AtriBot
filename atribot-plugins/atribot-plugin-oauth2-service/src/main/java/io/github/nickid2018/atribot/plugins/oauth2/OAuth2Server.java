package io.github.nickid2018.atribot.plugins.oauth2;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.nickid2018.atribot.util.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

@Slf4j
public class OAuth2Server {

    private HttpServer httpServer;

    public void startServer() {
        int serverPort = Configuration.getIntOrElse("oauth2.server.port", 8080);
        try {
            log.info("Starting OAuth2 Server on port {}", serverPort);
            httpServer = HttpServer.create(new InetSocketAddress(serverPort), 0);
            httpServer.setExecutor(Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setUncaughtExceptionHandler((th, t) -> log.error(
                        "Error in OAuth2 Server",
                        t
                    ))
                    .build()
            ));
            httpServer.start();
            log.info("OAuth2 Server started on port {}", serverPort);
        } catch (IOException e) {
            log.error("Error starting OAuth2 Server", e);
            httpServer = null;
        }
    }

    public void addHandler(String path, HttpHandler handler) {
        if (httpServer != null) {
            httpServer.createContext(path, handler);
            log.info("Handler added to path {}", path);
        }
    }

    public void removeHandler(String path) {
        if (httpServer != null) {
            httpServer.removeContext(path);
            log.info("Handler removed from path {}", path);
        }
    }

    public void stopServer() {
        if (httpServer != null) {
            httpServer.stop(0);
            log.info("OAuth2 Server stopped");
        }
    }
}
