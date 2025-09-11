package org.fergs.modules.impl.breachdetector;

import com.sun.javafx.util.Logging;
import okhttp3.*;
import org.fergs.managers.LoggingManager;
import org.fergs.objects.Breach;
import org.fergs.scheduler.SpecterScheduler;
import org.fergs.ui.forms.BreachGraphForm;
import org.fergs.ui.forms.SpecterForm;
import org.fergs.ui.notifications.ToastNotification;
import org.fergs.utils.BreachParser;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * AvastBreachDetectorImpl is a class that checks for data breaches associated with a given email address
 * using the Avast Identity Protection API. It supports proxy usage and displays results in the UI.
 */
public final class AvastBreachDetectorImpl {
    private static final LoggingManager LOGGER = LoggingManager.getInstance();
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final List<String> proxies;
    private final String proxyType;
    private final String targetEmail;
    private final JTextArea jTextArea;
    private boolean running = true;

    public AvastBreachDetectorImpl(List<String> proxies, String proxyType, String targetEmail, JTextArea jTextArea) {
        this.proxies = new ArrayList<>(proxies);
        this.proxyType = proxyType;
        this.targetEmail = targetEmail;
        this.jTextArea = jTextArea;
    }

    public void run() {
        Random rand = new Random();
        final int TIMEOUT_MS = 10_000;

        if (proxies.isEmpty()) {
            jTextArea.append("[LOG] No proxies found. Running without proxy.\n");
        } else {
            jTextArea.append("[LOG] Loaded " + proxies.size() + " proxies.\n");
        }

        while (running) {
            OkHttpClient client;
            String toDeleteProxy = null;

            jTextArea.append("[LOG] Starting Avast breach detection for: " + targetEmail + "\n");

            if (!"NONE".equalsIgnoreCase(proxyType) && !proxies.isEmpty()) {
                String proxyStr = proxies.get(rand.nextInt(proxies.size()));
                toDeleteProxy = proxyStr;

                String[] parts = proxyStr.split(":", 2);
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);

                Proxy.Type type = proxyType.startsWith("SOCKS")
                        ? Proxy.Type.SOCKS
                        : Proxy.Type.HTTP;
                Proxy proxy = new Proxy(type, new InetSocketAddress(host, port));

                client = new OkHttpClient.Builder()
                        .proxy(proxy)
                        .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                        .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                        .writeTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                        .build();

                jTextArea.append("[LOG] Using proxy: " + proxyStr + "\n");
            } else {
                client = new OkHttpClient.Builder()
                        .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                        .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                        .writeTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                        .build();
            }

            String jsonData = "{\"emailAddresses\":[\"" + targetEmail + "\"]}";
            Request request = new Request.Builder()
                    .url("https://identityprotection.avast.com/v1/web/query/site-breaches/unauthorized-data")
                    .post(RequestBody.create(jsonData, JSON))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .header("Vaar-Version", "0")
                    .header("Vaar-Header-App-Product-Name", "hackcheck-web-avast")
                    .header("Vaar-Header-App-Build-Version", "1.0.0")
                    .header("Accept", "*/*")
                    .header("Host", "identityprotection.avast.com")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    jTextArea.append("[ERROR] HTTP " + response.code() + " for " + targetEmail + "\n");
                    if (response.code() == 403 || response.code() == 429) {
                        jTextArea.append("[ERROR] Your IP might be temporarily blocked by Avast. Please try again later.\n");
                        running = false;
                    }
                    if (toDeleteProxy != null) {
                        proxies.remove(toDeleteProxy);
                        ToastNotification.builder(SpecterForm.frame)
                                .setBackground(new Color(0x2A2A2A))
                                .setTitleColor(new Color(0x00FF88))
                                .setMessageColor(new Color(0xF5F5F5))
                                .setTitleFont(new Font("JetBrains Mono", Font.BOLD, 16))
                                .setMessageFont(new Font("JetBrains Mono", Font.PLAIN, 13))
                                .setSize(255, 100)
                                .setFadeInStep(25)
                                .setFadeOutStep(35)
                                .setDuration(3500)
                                .setTitle("⚠︎ Proxy Removed")
                                .setMessage("Removed non-working proxy: " + toDeleteProxy)
                                .show();
                    }
                    LOGGER.log(Level.WARNING, "Non-successful HTTP response: " + response.code() + " for email: " + targetEmail);
                    continue;
                }
                String body = Objects.requireNonNull(response.body()).string();

                running = false;

                if (body.contains("breaches")) {
                    final List<Breach> breachesFound =  BreachParser.parse(body);
                    AtomicInteger count = new AtomicInteger(0);
                    if (breachesFound.size() > 5) {
                        SpecterScheduler.schedule(() -> ToastNotification.builder(SpecterForm.frame)
                                .setBackground(new Color(0x2A2A2A))
                                .setTitleColor(new Color(0x00FF88))
                                .setMessageColor(new Color(0xF5F5F5))
                                .setTitleFont(new Font("JetBrains Mono", Font.BOLD, 16))
                                .setMessageFont(new Font("JetBrains Mono", Font.PLAIN, 13))
                                .setSize(255, 100)
                                .setFadeInStep(25)
                                .setFadeOutStep(35)
                                .setDuration(3500)
                                .setTitle("☠︎ Breach(s) Detected")
                                .setMessage(
                                        "Multiple breaches detected for: " + targetEmail +
                                                "\nTotal Breaches: " + breachesFound.size()
                                )
                                .show(), 300L * count.getAndIncrement(), TimeUnit.MILLISECONDS);
                    } else {
                        for (Breach breach : breachesFound) {
                            SpecterScheduler.schedule(() -> ToastNotification.builder(SpecterForm.frame)
                                    .setBackground(new Color(0x2A2A2A))
                                    .setTitleColor(new Color(0x00FF88))
                                    .setMessageColor(new Color(0xF5F5F5))
                                    .setTitleFont(new Font("JetBrains Mono", Font.BOLD, 16))
                                    .setMessageFont(new Font("JetBrains Mono", Font.PLAIN, 13))
                                    .setSize(255, 100)
                                    .setFadeInStep(25)
                                    .setFadeOutStep(35)
                                    .setDuration(3500)
                                    .setTitle("☠︎ Breach Detected")
                                    .setMessage(
                                            "Website: " + breach.site() +
                                                    "\nDate: "    + breach.publishDate() +
                                                    "\nRecords: " + breach.recordsCount()
                                    )
                                    .show(), 300L * count.getAndIncrement(), TimeUnit.MILLISECONDS);

                            jTextArea.append("[LOG] Breach found: "
                                    + breach.site() + " on " + breach.publishDate() + "\n");
                        }
                    }

                    SwingUtilities.invokeLater(() -> {
                        BreachGraphForm win = new BreachGraphForm(targetEmail, breachesFound);
                        win.display();
                    });

                } else {
                    jTextArea.append("[LOG] No breaches found for: " + targetEmail + "\n");
                }



            } catch (IOException e) {
                if (toDeleteProxy != null) {
                    proxies.remove(toDeleteProxy);
                    ToastNotification.builder(SpecterForm.frame)
                            .setBackground(new Color(0x2A2A2A))
                            .setTitleColor(new Color(0x00FF88))
                            .setMessageColor(new Color(0xF5F5F5))
                            .setTitleFont(new Font("JetBrains Mono", Font.BOLD, 16))
                            .setMessageFont(new Font("JetBrains Mono", Font.PLAIN, 13))
                            .setSize(255, 100)
                            .setFadeInStep(25)
                            .setFadeOutStep(35)
                            .setDuration(3500)
                            .setTitle("⚠︎ Proxy Removed")
                            .setMessage("Removed non-working proxy: " + toDeleteProxy)
                            .show();
                } else {
                    e.printStackTrace();
                    break;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Exception in Breach Detected ", e);
                throw new RuntimeException(e);
            }
        }
    }
}
