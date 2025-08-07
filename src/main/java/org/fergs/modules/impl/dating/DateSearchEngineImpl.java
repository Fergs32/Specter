package org.fergs.modules.impl.dating;


import io.github.bonigarcia.wdm.WebDriverManager;
import org.fergs.objects.SearchResult;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.swing.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

//https://cse.google.com/cse?cx=c7b340447e1e12653&q={query}
public class DateSearchEngineImpl {
    private final List<String> proxies;
    private final String proxyType;
    private final String targetName;

    public DateSearchEngineImpl(List<String> proxies, String proxyType, String targetName) {
        this.proxies    = new ArrayList<>(proxies);
        this.proxyType  = proxyType;
        this.targetName = targetName;
    }

    public List<SearchResult> run() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions opts = new ChromeOptions()
                .addArguments("--headless","--disable-gpu","--window-size=1920,1080");
        applyProxy(opts);

        WebDriver driver = new ChromeDriver(opts);
        List<SearchResult> all = new ArrayList<>();

        try {
            String url = "https://cse.google.com/cse?cx=c7b340447e1e12653&q=" +
                    URLEncoder.encode(targetName, StandardCharsets.UTF_8);
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            for (;;) {
                try {
                    wait.until(ExpectedConditions
                            .presenceOfElementLocated(By.cssSelector(".gsc-webResult")));
                } catch (TimeoutException te) {
                    break;
                }

                List<WebElement> items = driver.findElements(
                        By.cssSelector(".gsc-webResult.gsc-result"));

                for (WebElement item : items) {
                    try {
                        WebElement linkEl = item.findElement(By.cssSelector(".gs-title a"));
                        String link = linkEl.getAttribute("href");
                        String title = linkEl.getText().trim();

                        String thumb = "";
                        List<WebElement> imgs = item.findElements(By.tagName("img"));
                        if (!imgs.isEmpty()) thumb = imgs.get(0).getAttribute("src");

                        all.add(new SearchResult(title, link, thumb));

                    } catch (Exception ie) {
                        System.err.println("Error processing item: " + ie.getMessage());
                    }
                }

                try {
                    List<WebElement> nextButtons = driver.findElements(By.cssSelector(".gsc-cursor-page[aria-label='Next']"));
                    if (!nextButtons.isEmpty() && nextButtons.get(0).isDisplayed()) {
                        nextButtons.get(0).click();
                        wait.until(ExpectedConditions.stalenessOf(items.get(0)));
                        continue;
                    }

                    WebElement currentPage = driver.findElement(By.cssSelector(".gsc-cursor-page.gsc-cursor-current-page"));
                    int currentPageNum = Integer.parseInt(currentPage.getText().trim());

                    List<WebElement> pageLinks = driver.findElements(By.cssSelector(".gsc-cursor-page[role='link']"));
                    Optional<WebElement> nextPage = pageLinks.stream()
                            .filter(el -> {
                                try {
                                    return Integer.parseInt(el.getText().trim()) > currentPageNum;
                                } catch (NumberFormatException e) {
                                    return false;
                                }
                            })
                            .findFirst();

                    if (nextPage.isPresent()) {
                        nextPage.get().click();
                        wait.until(ExpectedConditions.stalenessOf(items.get(0)));
                    } else {
                        break;
                    }
                } catch (NoSuchElementException nse) {
                    break;
                }
            }
            return all;

        } catch (Exception e) {
            return all;
        } finally {
            driver.quit();
        }
    }

    private void applyProxy(ChromeOptions opts) {
        if (!"NONE".equalsIgnoreCase(proxyType) && !proxies.isEmpty()) {
            String p = proxies.get(new Random().nextInt(proxies.size()));
            String[] parts = p.split(":", 2);
            String host = parts[0], port = parts[1];
            org.openqa.selenium.Proxy selProxy = new org.openqa.selenium.Proxy();
            selProxy.setProxyType(org.openqa.selenium.Proxy.ProxyType.MANUAL);
            if (proxyType.startsWith("SOCKS")) {
                selProxy.setSocksProxy(host + ":" + port)
                        .setSocksVersion(proxyType.equals("SOCKS5") ? 5 : 4);
            } else {
                selProxy.setHttpProxy(host + ":" + port)
                        .setSslProxy(host + ":" + port);
            }
            opts.setProxy(selProxy);
        }
    }
}
