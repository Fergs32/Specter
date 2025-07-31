package org.fergs.modules.impl.breachdetector;

import okhttp3.*;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class AvastBreachDetectorImpl {
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final List<String> proxies;
    private final String proxyType;
    private final String targetEmail;

    public AvastBreachDetectorImpl(List<String> proxies, String proxyType, String targetEmail) {
        this.proxies = new ArrayList<>(proxies);
        this.proxyType = proxyType;
        this.targetEmail = targetEmail;
    }

    public AvastBreachDetectorImpl(String proxyType, String targetEmail) {
        this.proxies = new ArrayList<>();
        this.proxyType = proxyType;
        this.targetEmail = targetEmail;
    }

    public void run() {
        Random rand = new Random();
        final int TIMEOUT_MS = 10_000;

        while (true) {
            OkHttpClient client;
            String toDeleteProxy = null;

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

                System.out.printf("Using proxy: %s%n", proxyStr);
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
                    throw new IOException("Unexpected code " + response);
                }
                String body = Objects.requireNonNull(response.body()).string();

                System.out.println("is this shit api patched? lets find out! = " + body);


            } catch (IOException e) {
                // on network error, remove bad proxy and retry
                if (toDeleteProxy != null) {
                    proxies.remove(toDeleteProxy);
                    System.err.println("Removed bad proxy: " + toDeleteProxy);
                } else {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
}
