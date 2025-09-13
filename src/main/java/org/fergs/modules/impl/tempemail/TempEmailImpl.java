package org.fergs.modules.impl.tempemail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.fergs.managers.LoggingManager;
import org.fergs.objects.TempEmail;
import org.fergs.objects.TempMessage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * TempEmailImpl handles the actual API interactions with the mail.tm service.
 * It provides methods to create temporary emails, fetch messages, and manage email accounts.
 * Uses OkHttp for HTTP requests and Jackson for JSON parsing.
 *
 * @Author Fergs32
 */
public final class TempEmailImpl {
    private static final String BASE_URL = "https://api.mail.tm";
    private static final LoggingManager LOGGER = LoggingManager.getInstance();
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public TempEmailImpl() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Creates a new temporary email address.
     * First gets available domains, then creates an account with a random username.
     */
    public TempEmail createTempEmail() throws Exception {
        LOGGER.info("Creating new temporary email address");

        List<String> domains = getAvailableDomains();
        if (domains.isEmpty()) {
            throw new RuntimeException("No available domains found");
        }

        String domain = domains.getFirst();
        String username = generateRandomUsername();
        String address = username + "@" + domain;
        String password = generateRandomPassword();

        LOGGER.info("Attempting to create email: " + address);

        String requestBody = String.format(
            "{\"address\":\"%s\",\"password\":\"%s\"}",
            address, password
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "/accounts")
                .post(RequestBody.create(requestBody, JSON))
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                LOGGER.error("Failed to create email account. HTTP " + response.code() + ": " + errorBody);
                throw new RuntimeException("Failed to create email: HTTP " + response.code());
            }

            String responseBody = response.body().string();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);

            String id = jsonResponse.get("id").asText();
            String createdAt = jsonResponse.get("createdAt").asText();

            LOGGER.info("Email account created successfully: " + address);

            String token = authenticateAccount(address, password);

            return new TempEmail(
                id,
                address,
                password,
                token,
                parseDateTime(createdAt)
            );
        }
    }

    /**
     * Authenticates with the created account to get a JWT token.
     */
    private String authenticateAccount(String address, String password) throws Exception {
        LOGGER.info("Authenticating account: " + address);

        String requestBody = String.format(
            "{\"address\":\"%s\",\"password\":\"%s\"}",
            address, password
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "/token")
                .post(RequestBody.create(requestBody, JSON))
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                LOGGER.error("Failed to authenticate account. HTTP " + response.code() + ": " + errorBody);
                throw new RuntimeException("Failed to authenticate: HTTP " + response.code());
            }

            String responseBody = response.body().string();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);

            String token = jsonResponse.get("token").asText();
            LOGGER.info("Account authenticated successfully");

            return token;
        }
    }

    /**
     * Retrieves available email domains from the mail.tm service.
     */
    private List<String> getAvailableDomains() throws Exception {
        LOGGER.info("Fetching available domains");

        Request request = new Request.Builder()
                .url(BASE_URL + "/domains")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOGGER.error("Failed to fetch domains. HTTP " + response.code());
                throw new RuntimeException("Failed to fetch domains: HTTP " + response.code());
            }

            String responseBody = response.body().string();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);

            List<String> domains = new ArrayList<>();
            JsonNode domainsArray = jsonResponse.get("hydra:member");

            if (domainsArray != null && domainsArray.isArray()) {
                for (JsonNode domainNode : domainsArray) {
                    String domain = domainNode.get("domain").asText();
                    domains.add(domain);
                }
            }

            LOGGER.info("Found " + domains.size() + " available domains");
            return domains;
        }
    }

    /**
     * Retrieves messages for a specific email account.
     */
    public List<TempMessage> getMessages(String accountId, String token) throws Exception {
        LOGGER.info("Fetching messages for account: " + accountId);

        Request request = new Request.Builder()
                .url(BASE_URL + "/messages")
                .get()
                .header("Authorization", "Bearer " + token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                if (response.code() == 401) {
                    LOGGER.error("Authentication failed for account: " + accountId);
                    throw new RuntimeException("Authentication failed");
                }
                LOGGER.error("Failed to fetch messages. HTTP " + response.code());
                throw new RuntimeException("Failed to fetch messages: HTTP " + response.code());
            }

            String responseBody = response.body().string();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);

            List<TempMessage> messages = new ArrayList<>();
            JsonNode messagesArray = jsonResponse.get("hydra:member");

            if (messagesArray != null && messagesArray.isArray()) {
                for (JsonNode messageNode : messagesArray) {
                    TempMessage message = parseMessage(messageNode);
                    messages.add(message);
                }
            }

            LOGGER.info("Found " + messages.size() + " messages for account: " + accountId);
            return messages;
        }
    }

    /**
     * Retrieves the full content of a specific message.
     */
    public String getMessageContent(String accountId, String messageId, String token) throws Exception {
        LOGGER.info("Fetching content for message: " + messageId);

        Request request = new Request.Builder()
                .url(BASE_URL + "/messages/" + messageId)
                .get()
                .header("Authorization", "Bearer " + token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOGGER.error("Failed to fetch message content. HTTP " + response.code());
                throw new RuntimeException("Failed to fetch message content: HTTP " + response.code());
            }

            String responseBody = response.body().string();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);

            JsonNode htmlNode = jsonResponse.get("html");
            JsonNode textNode = jsonResponse.get("text");

            if (htmlNode != null && !htmlNode.isNull() && htmlNode.isArray() && !htmlNode.isEmpty()) {
                return stripHtmlTags(htmlNode.get(0).asText());
            } else if (textNode != null && !textNode.isNull() && textNode.isArray() && !textNode.isEmpty()) {
                return textNode.get(0).asText();
            } else {
                return "No content available";
            }
        }
    }

    /**
     * Deletes a temporary email account.
     */
    public boolean deleteEmail(String accountId, String token) throws Exception {
        LOGGER.info("Deleting email account: " + accountId);

        Request request = new Request.Builder()
                .url(BASE_URL + "/accounts/" + accountId)
                .delete()
                .header("Authorization", "Bearer " + token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            boolean success = response.isSuccessful();
            if (success) {
                LOGGER.info("Email account deleted successfully: " + accountId);
            } else {
                LOGGER.error("Failed to delete email account. HTTP " + response.code());
            }
            return success;
        }
    }

    /**
     * Parses a message JSON node into a TempMessage object.
     */
    private TempMessage parseMessage(JsonNode messageNode) {
        String id = messageNode.get("id").asText();
        String subject = messageNode.has("subject") ? messageNode.get("subject").asText() : "No Subject";

        String from = "Unknown Sender";
        JsonNode fromNode = messageNode.get("from");
        if (fromNode != null && fromNode.has("address")) {
            from = fromNode.get("address").asText();
        }

        String createdAt = messageNode.get("createdAt").asText();
        boolean isRead = messageNode.has("seen") ? messageNode.get("seen").asBoolean() : false;

        return new TempMessage(
            id,
            subject,
            from,
            parseDateTime(createdAt),
            isRead
        );
    }

    /**
     * Parses ISO 8601 datetime string to LocalDateTime.
     */
    private LocalDateTime parseDateTime(String dateTimeString) {
        try {
            if (dateTimeString.contains("+")) {
                dateTimeString = dateTimeString.substring(0, dateTimeString.indexOf('+'));
            } else if (dateTimeString.contains("Z")) {
                dateTimeString = dateTimeString.replace("Z", "");
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            return LocalDateTime.parse(dateTimeString, formatter);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse datetime: " + dateTimeString, e);
            return LocalDateTime.now();
        }
    }

    /**
     * Generates a random username for email addresses.
     */
    private String generateRandomUsername() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();

        // Generate random username of 8-12 characters
        int length = 8 + (int)(Math.random() * 5);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt((int)(Math.random() * chars.length())));
        }

        return sb.toString();
    }

    /**
     * Generates a random password for email accounts.
     */
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder sb = new StringBuilder();

        int length = 12 + (int)(Math.random() * 5);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt((int)(Math.random() * chars.length())));
        }

        return sb.toString();
    }

    /**
     * Strips HTML tags from content for display in plain text areas.
     */
    private String stripHtmlTags(String html) {
        if (html == null) return "";

        return html
                .replaceAll("<br[^>]*>", "\n")
                .replaceAll("<p[^>]*>", "\n")
                .replaceAll("</p>", "\n")
                .replaceAll("<div[^>]*>", "\n")
                .replaceAll("</div>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .replaceAll("\\n\\s*\\n", "\n\n")
                .trim();
    }
}
