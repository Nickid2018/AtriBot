package io.github.nickid2018.atribot.plugins.webrenderer;

import io.github.nickid2018.atribot.core.communicate.Communicate;
import io.github.nickid2018.atribot.core.communicate.CommunicateReceiver;
import io.github.nickid2018.atribot.network.file.TransferFileResolver;
import io.github.nickid2018.atribot.util.Configuration;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@AllArgsConstructor
public class WebRendererReceiver implements CommunicateReceiver {

    private final WebRendererPlugin plugin;

    @Communicate("webrenderer.render_page_element")
    public CompletableFuture<?> renderElement(Map<String, Object> data) {
        String page = (String) data.get("page");
        int timeout = (int) data.getOrDefault("timeout", 30);
        String elementSelector = (String) data.getOrDefault("element", "html");
        IntIntPair windowSize = data.containsKey("windowSize") ? (IntIntPair) data.get("windowSize") : null;
        return CompletableFuture.supplyAsync(() -> {
            RemoteWebDriver driver = plugin.createNewDriver();
            try {
                if (windowSize != null)
                    driver.manage().window().setSize(new Dimension(windowSize.leftInt(), windowSize.rightInt()));
                else
                    driver.manage().window().maximize();

                driver.get(page);
                log.debug("Rendering page {} with element '{}'", page, elementSelector);
                WebDriverWait wait = new WebDriverWait(driver, Duration.ZERO);
                wait.withTimeout(Duration.of(timeout, ChronoUnit.SECONDS));
                try {
                    wait.until(d -> ((JavascriptExecutor) d).executeScript(
                        """
                        if (document.readyState !== 'complete')
                            return false;
                        const images = document.images
                        for (let i = 0; i < images.length; i++) {
                            if (!images[i].complete)
                                return false;
                        }
                        return true;
                        """
                    ));
                } catch (TimeoutException timeoutException) {
                    log.warn("Page {} loading timeout", page);
                }
                log.debug("Page {} loaded", page);

                By element = By.cssSelector(elementSelector);
                WebElement webElement = driver.findElement(element);
                return webElement.getScreenshotAs(OutputType.BYTES);
            } finally {
                driver.quit();
            }
        }, plugin.getRendererExecutor());
    }

    @Communicate("webrenderer.render_html_element")
    public CompletableFuture<?> renderHtmlElement(Map<String, Object> data) {
        String html = (String) data.get("html");

        return TransferFileResolver
            .transferFile(
                Configuration.getStringOrElse("webrenderer.transfer_type", "file"),
                Configuration.getStringOrElse("webrenderer.transfer_arg", ".=>."),
                new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)),
                plugin.getExecutorService()
            )
            .thenApply(uri -> {
                data.put("page", uri);
                return data;
            })
            .thenCompose(this::renderElement);
    }
}
