package org.fergs.modules.impl.finders;

import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DatabaseFinderImpl {
    private static final List<String> FLOWERS = List.of(
            "inurl:sinister.ly",
            "inurl:cracking.org",
            "inurl:nulledbb.com",
            "inurl:altenens.is",
            "inurl:patched.to"
    );

    private final List<String> proxies;
    private final String proxyType;
    private final String target;
    private final JTextArea logArea;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public DatabaseFinderImpl(List<String> proxies, String proxyType, String target, JTextArea logArea) {
        this.proxies = new ArrayList<>(proxies);
        this.proxyType = proxyType.toUpperCase();
        this.target = target;
        this.logArea = logArea;
    }

    public void stop() {
        running.set(false);
    }

    public void run() {
        SwingUtilities.invokeLater(() -> logArea.append("[LOG] Starting DB finder for: " + target + "\n"));

        Random rand = new Random();
        AtomicInteger flowerIdx = new AtomicInteger(0);

        while (running.get() && flowerIdx.get() < FLOWERS.size()) {
            String flower = FLOWERS.get(flowerIdx.getAndIncrement());
            String query  = flower + " \"" + target + "\"";
            String url    = "https://www.google.com/search?q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&num=100&hl=en&complete=0&safe=off&filter=0&start=0";

            SwingUtilities.invokeLater(() ->
                    logArea.append("[LOG] Querying Google: " + query + "\n")
            );

            OkHttpClient client = buildClient(rand);
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .get()
                    .build();

            try (Response resp = client.newCall(request).execute()) {
                if (!resp.isSuccessful()) {
                    if (resp.code() == 429) {
                        SwingUtilities.invokeLater(() ->
                                logArea.append("[ERROR] Your IP is temporarily blocked by Google. Please try again later.\n")
                        );
                        running.set(false);
                        return;
                    }
                    SwingUtilities.invokeLater(() ->
                            logArea.append("[ERROR] HTTP " + resp.code() + " for " + flower + "\n")
                    );
                    continue;
                }

                assert resp.body() != null;

                String html = resp.body().string();
                List<String> links = parseGoogleResults(html);

                if (links.isEmpty()) {
                    SwingUtilities.invokeLater(() ->
                            logArea.append("[LOG] No results for " + flower + "\n")
                    );
                } else {
                    for (String link : links) {
                        SwingUtilities.invokeLater(() ->
                                logArea.append("[FOUND] " + link + "\n")
                        );
                    }
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() ->
                        logArea.append("[ERROR] Exception: " + e.getMessage() + "\n")
                );
            }

            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        }

        SwingUtilities.invokeLater(() ->
                logArea.append("[LOG] Database finder finished.\n")
        );
    }

    private OkHttpClient buildClient(Random rand) {
        OkHttpClient.Builder b = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(10));

        if (!"NONE".equals(proxyType) && !proxies.isEmpty()) {
            String proxyStr = proxies.get(rand.nextInt(proxies.size()));
            String[] parts = proxyStr.split(":", 2);
            Proxy.Type type = proxyType.startsWith("SOCKS")
                    ? Proxy.Type.SOCKS
                    : Proxy.Type.HTTP;
            Proxy proxy = new Proxy(type,
                    new InetSocketAddress(parts[0], Integer.parseInt(parts[1]))
            );
            b.proxy(proxy);
            SwingUtilities.invokeLater(() ->
                    logArea.append("[LOG] Using proxy: " + proxyStr + "\n")
            );
        }

        return b.build();
    }

    private List<String> parseGoogleResults(String html) {
        List<String> results = new ArrayList<>();
        Document doc = Jsoup.parse(html);

        Elements anchors = doc.select("div.yuRUbf > a");
        for (Element a : anchors) {
            String href = a.absUrl("href");
            if (!href.isEmpty()) {
                results.add(href);
            }
        }
        return results;
    }
}
