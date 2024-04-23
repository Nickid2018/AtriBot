package io.github.nickid2018.atribot.plugins.webrenderer;

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
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@AllArgsConstructor
public class WebRendererReceiver implements CommunicateReceiver {

    private static final Set<String> KEY = Set.of("webrenderer.render_page_element", "webrenderer.render_html_element");
    private final WebRendererPlugin plugin;

    @Override
    @SuppressWarnings("unchecked")
    public <T, D> CompletableFuture<T> communicate(String communicateKey, D data) {
        Map<String, Object> map = (Map<String, Object>) data;
        switch (communicateKey) {
            case "webrenderer.render_page_element" -> {
                return renderElement(map);
            }
            case "webrenderer.render_html_element" -> {
                return renderHtmlElement(map);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> renderElement(Map<String, Object> data) {
        String page = (String) data.get("page");
        String elementSelector = (String) data.getOrDefault("element", "html");
        IntIntPair windowSize = data.containsKey("windowSize") ? (IntIntPair) data.get("windowSize") : null;
        return (CompletableFuture<T>) CompletableFuture.supplyAsync(() -> {
            RemoteWebDriver driver = plugin.createNewDriver();

            if (windowSize != null)
                driver.manage().window().setSize(new Dimension(windowSize.leftInt(), windowSize.rightInt()));
            else
                driver.manage().window().maximize();

            driver.get(page);
            log.debug("Rendering page {} with element '{}'", page, elementSelector);
            WebDriverWait wait = new WebDriverWait(driver, Duration.of(10, ChronoUnit.SECONDS));
            wait.until(d -> ((JavascriptExecutor) d).executeScript(
                "return document.readyState"
            ).equals("complete"));
            log.debug("Page {} loaded", page);

            By element = By.cssSelector(elementSelector);
            WebElement webElement = driver.findElement(element);
            byte[] screenshot = webElement.getScreenshotAs(OutputType.BYTES);

            driver.quit();

            return screenshot;
        }, plugin.getRendererExecutor());
    }

    private <T> CompletableFuture<T> renderHtmlElement(Map<String, Object> data) {
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

    @Override
    public Set<String> availableCommunicateKeys() {
        return KEY;
    }
}
