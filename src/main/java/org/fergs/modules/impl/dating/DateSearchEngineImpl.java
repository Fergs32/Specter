package org.fergs.modules.impl.dating;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.fergs.managers.LoggingManager;
import org.fergs.objects.SearchResult;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.swing.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * DateSearchEngineImpl is a class that performs web searches using Google Custom Search Engine (CSE)
 * to find dating-related results for a specified target name. It supports proxy usage, headless
 * browser operation using Selenium WebDriver with Chrome, and various filtering options.
 * <p>
 * Leverages a custom Google CSE configured for dating searches with enhanced filtering capabilities.
 * <p>
 *
 * @Author Fergs32
 */
public final class DateSearchEngineImpl {
    private final LoggingManager LOGGER = LoggingManager.getInstance();
    private final List<String> proxies;
    private final String proxyType;
    private final String targetName;

    // Filter parameters
    private final String ageRange;
    private final String location;
    private final String platform;
    private final String sortBy;
    private final int maxResults;
    private final boolean photosOnly;
    private final boolean verifiedOnly;

    public DateSearchEngineImpl(List<String> proxies, String proxyType, String targetName) {
        this(proxies, proxyType, targetName, "Any", "Any", "All", "Relevance", 50, false, false);
    }

    public DateSearchEngineImpl(List<String> proxies, String proxyType, String targetName,
                                String ageRange, String location, String platform, String sortBy,
                                int maxResults, boolean photosOnly, boolean verifiedOnly) {
        this.proxies = new ArrayList<>(proxies);
        this.proxyType = proxyType;
        this.targetName = targetName;
        this.ageRange = ageRange;
        this.location = location;
        this.platform = platform;
        this.sortBy = sortBy;
        this.maxResults = maxResults;
        this.photosOnly = photosOnly;
        this.verifiedOnly = verifiedOnly;
    }

    public List<SearchResult> run() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions opts = new ChromeOptions()
                .addArguments("--headless","--disable-gpu","--window-size=1920,1080");
        applyProxy(opts);

        WebDriver driver = new ChromeDriver(opts);
        List<SearchResult> all = new ArrayList<>();

        try {
            String searchQuery = buildSearchQuery();
            String url = "https://cse.google.com/cse?cx=c7b340447e1e12653&q=" +
                    URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);


            LOGGER.log(Level.INFO, "Searching for: {0}", searchQuery);
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

                        SearchResult result = new SearchResult(title, link, thumb);

                        // Apply client-side filtering
                        if (shouldIncludeResult(result)) {
                            all.add(result);
                        }

                        // Stop if we've reached max results
                        if (all.size() >= maxResults) {
                            return all;
                        }

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

            return applySorting(all);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during search: {0}", e.getMessage());
            return all;
        } finally {
            driver.quit();
        }
    }

    private String buildSearchQuery() {
        StringBuilder query = new StringBuilder(targetName);

        // Add platform-specific search terms
        if (!"All".equals(platform)) {
            query.append(" site:").append(getPlatformSite(platform));
        }

        // Add age-related search terms
        if (!"Any".equals(ageRange)) {
            query.append(" \"").append(ageRange).append("\"");
        }

        // Add location-based terms
        if (!"Any".equals(location) && !location.startsWith("Within")) {
            query.append(" \"").append(location).append("\"");
        }

        // Add photo-related terms
        if (photosOnly) {
            query.append(" (photo OR picture OR image)");
        }

        // Add verification terms
        if (verifiedOnly) {
            query.append(" (verified OR authentic)");
        }

        return query.toString();
    }

    private String getPlatformSite(String platform) {
        return switch (platform.toLowerCase()) {
            case "tinder" -> "tinder.com";
            case "bumble" -> "bumble.com";
            case "hinge" -> "hinge.co";
            case "match" -> "match.com";
            case "pof" -> "pof.com";
            case "okcupid" -> "okcupid.com";
            case "badoo" -> "badoo.com";
            default -> "";
        };
    }

    private boolean shouldIncludeResult(SearchResult result) {
        // Apply photos only filter
        if (photosOnly && (result.thumbnail == null || result.thumbnail.isEmpty())) {
            return false;
        }

        // Apply platform filter (check if URL contains platform domain)
        if (!"All".equals(platform)) {
            String platformSite = getPlatformSite(platform);
            if (!platformSite.isEmpty() && !result.url.toLowerCase().contains(platformSite)) {
                return false;
            }
        }

        // Apply verification filter (check title/URL for verification indicators)
        if (verifiedOnly) {
            String titleLower = result.title.toLowerCase();
            String urlLower = result.url.toLowerCase();
            if (!titleLower.contains("verified") && !titleLower.contains("authentic") &&
                    !urlLower.contains("verified") && !urlLower.contains("authentic")) {
                return false;
            }
        }

        return true;
    }

    private List<SearchResult> applySorting(List<SearchResult> results) {
        return switch (sortBy) {
            case "Most Recent" -> results.stream()
                    .sorted((a, b) -> b.url.compareTo(a.url)) // Simple URL-based sorting
                    .collect(Collectors.toList());
            case "Distance" -> results.stream()
                    .sorted(Comparator.comparingInt(a -> a.title.length())) // Title length as proxy
                    .collect(Collectors.toList());
            case "Activity" -> results.stream()
                    .sorted((a, b) -> Boolean.compare(
                            b.thumbnail != null && !b.thumbnail.isEmpty(),
                            a.thumbnail != null && !a.thumbnail.isEmpty())) // Results with images first
                    .collect(Collectors.toList());
            case "Profile Quality" -> results.stream()
                    .sorted((a, b) -> Integer.compare(b.title.length(), a.title.length())) // Longer titles = better quality
                    .collect(Collectors.toList());
            default -> results; // Relevance (original order)
        };
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

    // Getters for filter parameters
    public String getAgeRange() { return ageRange; }
    public String getLocation() { return location; }
    public String getPlatform() { return platform; }
    public String getSortBy() { return sortBy; }
    public int getMaxResults() { return maxResults; }
    public boolean isPhotosOnly() { return photosOnly; }
    public boolean isVerifiedOnly() { return verifiedOnly; }
}