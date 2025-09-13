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

        final WebDriver driver = new ChromeDriver(opts);
        final List<SearchResult> all = new ArrayList<>();
        final Set<String> seenUrls = new HashSet<>();

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

                        assert link != null;

                        // Skip URLs containing /places/
                        if (link.contains("/places/")) continue;

                        // Skip duplicate URLs
                        if (seenUrls.contains(link)) continue;

                        seenUrls.add(link); // Add to seen URLs set

                        SearchResult result = new SearchResult(title, link, thumb);

                        if (shouldIncludeResult(result)) {
                            all.add(result);
                        }

                        if (all.size() >= maxResults) {
                            return applySorting(all);
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

        if (!"All".equals(platform)) {
            String platformSite = getPlatformSite(platform);
            if (!platformSite.isEmpty()) {
                query.append(" site:").append(platformSite);
            }
        }

        if (!"Any".equals(ageRange)) {
            query.append(" (");
            query.append("\"").append(ageRange).append("\"");

            String[] ageParts = ageRange.split("-");
            if (ageParts.length == 2) {
                query.append(" OR \"age ").append(ageParts[0]).append("\"");
                query.append(" OR \"").append(ageParts[0]).append(" years old\"");
                query.append(" OR \"").append(ageParts[1]).append(" years old\"");
            } else if (ageRange.equals("55+")) {
                query.append(" OR \"55 years old\" OR \"60 years old\" OR \"mature\"");
            }
            query.append(")");
        }

        if (!"Any".equals(location)) {
            query.append(" (");
            if (location.startsWith("Within")) {
                String miles = location.replaceAll("[^0-9]", "");
                query.append("\"within ").append(miles).append(" miles\"");
                query.append(" OR \"").append(miles).append(" miles away\"");
                query.append(" OR \"nearby\" OR \"local\"");
            } else if (location.equals("Same city")) {
                query.append("\"same city\" OR \"local\" OR \"in my city\"");
            } else if (location.equals("Same state")) {
                query.append("\"same state\" OR \"in state\" OR \"local area\"");
            } else {
                query.append("\"").append(location).append("\"");
            }
            query.append(")");
        }

        // Add photo-related terms with more variations
        if (photosOnly) {
            query.append(" (photo OR picture OR image OR pics OR \"profile pic\" OR \"profile photo\" OR selfie)");
        }

        // Add verification terms with more comprehensive patterns
        if (verifiedOnly) {
            query.append(" (verified OR authentic OR \"verified profile\" OR \"real profile\" OR confirmed OR validated)");
        }

        // Add dating-specific keywords to improve relevance
        query.append(" (dating OR profile OR single OR \"looking for\" OR relationship OR match)");

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
        String titleLower = result.title.toLowerCase();
        String urlLower = result.url.toLowerCase();

        // Apply photos only filter
        if (photosOnly && (result.thumbnail == null || result.thumbnail.isEmpty())) {
            return false;
        }

        if (!"All".equals(platform)) {
            String platformSite = getPlatformSite(platform);
            if (!platformSite.isEmpty() && !urlLower.contains(platformSite)) {
                return false;
            }
        }

        if (!"Any".equals(ageRange)) {
            boolean ageMatch = false;

            if (ageRange.equals("55+")) {
                // Check for 55+ indicators
                ageMatch = titleLower.matches(".*\\b(5[5-9]|[6-9]\\d)\\b.*") ||
                          titleLower.contains("mature") ||
                          titleLower.contains("senior") ||
                          titleLower.contains("older");
            } else {
                String[] ageParts = ageRange.split("-");
                if (ageParts.length == 2) {
                    int minAge = Integer.parseInt(ageParts[0]);
                    int maxAge = Integer.parseInt(ageParts[1]);

                    for (int age = minAge; age <= maxAge; age++) {
                        if (titleLower.contains(String.valueOf(age)) || urlLower.contains(String.valueOf(age))) {
                            ageMatch = true;
                            break;
                        }
                    }

                    if (titleLower.contains(ageRange) || urlLower.contains(ageRange)) {
                        ageMatch = true;
                    }
                }
            }

            if (!ageMatch) {
                return false;
            }
        }

        if (!"Any".equals(location)) {
            boolean locationMatch = false;

            if (location.startsWith("Within")) {
                locationMatch = titleLower.contains("nearby") ||
                               titleLower.contains("local") ||
                               titleLower.contains("miles") ||
                               titleLower.contains("close") ||
                               urlLower.contains("local");
            } else if (location.equals("Same city")) {
                locationMatch = titleLower.contains("city") ||
                               titleLower.contains("local") ||
                               titleLower.contains("downtown") ||
                               titleLower.contains("metro");
            } else if (location.equals("Same state")) {
                locationMatch = titleLower.contains("state") ||
                               titleLower.contains("region") ||
                               titleLower.contains("area");
            }

            // If we're filtering by location and don't find indicators, exclude
            if (!locationMatch) {
                return false;
            }
        }

        // Apply verification filter with enhanced patterns
        if (verifiedOnly) {
            boolean verificationMatch = titleLower.contains("verified") ||
                                       titleLower.contains("authentic") ||
                                       titleLower.contains("confirmed") ||
                                       titleLower.contains("validated") ||
                                       titleLower.contains("real") ||
                                       titleLower.contains("genuine") ||
                                       urlLower.contains("verified") ||
                                       urlLower.contains("authentic") ||
                                       urlLower.contains("confirmed");

            if (!verificationMatch) {
                return false;
            }
        }

        // Additional quality filters - exclude obviously irrelevant results
        if (titleLower.contains("spam") || titleLower.contains("fake") ||
            titleLower.contains("scam") || titleLower.contains("bot")) {
            return false;
        }

        return true;
    }

    private List<SearchResult> applySorting(List<SearchResult> results) {
        return switch (sortBy) {
            case "Most Recent" -> results.stream()
                    .sorted((a, b) -> {
                        int scoreA = getRecencyScore(a);
                        int scoreB = getRecencyScore(b);
                        return Integer.compare(scoreB, scoreA); // Higher score = more recent
                    })
                    .collect(Collectors.toList());

            case "Distance" -> results.stream()
                    .sorted((a, b) -> {
                        int distanceScoreA = getDistanceScore(a);
                        int distanceScoreB = getDistanceScore(b);
                        return Integer.compare(distanceScoreB, distanceScoreA); // Higher score = closer
                    })
                    .collect(Collectors.toList());

            case "Activity" -> results.stream()
                    .sorted((a, b) -> {
                        int activityScoreA = getActivityScore(a);
                        int activityScoreB = getActivityScore(b);
                        return Integer.compare(activityScoreB, activityScoreA); // Higher score = more active
                    })
                    .collect(Collectors.toList());

            case "Profile Quality" -> results.stream()
                    .sorted((a, b) -> {
                        int qualityScoreA = getProfileQualityScore(a);
                        int qualityScoreB = getProfileQualityScore(b);
                        return Integer.compare(qualityScoreB, qualityScoreA); // Higher score = better quality
                    })
                    .collect(Collectors.toList());

            default -> results; // Relevance (original order from search engine)
        };
    }

    private int getRecencyScore(SearchResult result) {
        String titleLower = result.title.toLowerCase();
        String urlLower = result.url.toLowerCase();
        int score = 0;

        // Look for recent time indicators
        if (titleLower.contains("today") || titleLower.contains("now")) score += 10;
        if (titleLower.contains("recent") || titleLower.contains("new")) score += 8;
        if (titleLower.contains("online") || titleLower.contains("active")) score += 6;
        if (titleLower.contains("2024") || titleLower.contains("2025")) score += 5;
        if (titleLower.contains("updated") || titleLower.contains("fresh")) score += 4;

        // URL patterns that suggest recent content
        if (urlLower.contains("2024") || urlLower.contains("2025")) score += 3;
        if (urlLower.contains("recent") || urlLower.contains("new")) score += 2;

        return score;
    }

    private int getDistanceScore(SearchResult result) {
        String titleLower = result.title.toLowerCase();
        String urlLower = result.url.toLowerCase();
        int score = 0;

        // Proximity indicators (higher score = closer/better)
        if (titleLower.contains("nearby") || titleLower.contains("close")) score += 10;
        if (titleLower.contains("local")) score += 8;
        if (titleLower.contains("miles")) score += 6;
        if (titleLower.contains("city") || titleLower.contains("downtown")) score += 4;
        if (titleLower.contains("area") || titleLower.contains("region")) score += 3;

        // URL indicators
        if (urlLower.contains("local")) score += 2;

        return score;
    }

    private int getActivityScore(SearchResult result) {
        String titleLower = result.title.toLowerCase();
        String urlLower = result.url.toLowerCase();
        int score = 0;

        // Image presence (strong activity indicator)
        if (result.thumbnail != null && !result.thumbnail.isEmpty()) score += 15;

        // Activity keywords
        if (titleLower.contains("online") || titleLower.contains("active")) score += 10;
        if (titleLower.contains("recently") || titleLower.contains("today")) score += 8;
        if (titleLower.contains("messages") || titleLower.contains("chat")) score += 6;
        if (titleLower.contains("responses") || titleLower.contains("replies")) score += 5;
        if (titleLower.contains("views") || titleLower.contains("visits")) score += 4;
        if (titleLower.contains("likes") || titleLower.contains("matches")) score += 3;

        // Platform activity indicators
        if (urlLower.contains("profile") && urlLower.contains("active")) score += 5;

        return score;
    }

    private int getProfileQualityScore(SearchResult result) {
        String titleLower = result.title.toLowerCase();
        String urlLower = result.url.toLowerCase();
        int score = 0;

        score += Math.min(result.title.length() / 10, 15);

        if (result.thumbnail != null && !result.thumbnail.isEmpty()) score += 20;

        if (titleLower.contains("verified") || titleLower.contains("authentic")) score += 25;
        if (titleLower.contains("premium") || titleLower.contains("plus")) score += 15;

        if (titleLower.contains("photos") || titleLower.contains("pictures")) score += 10;
        if (titleLower.contains("education") || titleLower.contains("profession")) score += 8;
        if (titleLower.contains("interests") || titleLower.contains("hobbies")) score += 6;
        if (titleLower.contains("about") || titleLower.contains("description")) score += 5;

        if (titleLower.contains("reviews") || titleLower.contains("ratings")) score += 12;
        if (titleLower.contains("connections") || titleLower.contains("matches")) score += 8;

        if (titleLower.contains("incomplete") || titleLower.contains("basic")) score -= 10;
        if (titleLower.contains("limited") || titleLower.contains("partial")) score -= 5;

        if (urlLower.contains("premium") || urlLower.contains("verified")) score += 5;
        if (urlLower.contains("profile") && urlLower.contains("complete")) score += 3;

        return Math.max(score, 0); // Ensure non-negative score
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
