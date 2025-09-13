package org.fergs.objects;

import java.time.LocalDateTime;

/**
 * Represents a message received in a temporary email inbox.
 * This record holds all the metadata and content information for an email message.
 *
 * @param id         The unique identifier for the message
 * @param subject    The subject line of the email
 * @param from       The sender's email address
 * @param receivedAt The timestamp when the message was received
 * @param isRead     Whether the message has been read/viewed
 *
 * @Author Fergs32
 */
public record TempMessage(
    String id,
    String subject,
    String from,
    LocalDateTime receivedAt,
    boolean isRead
) {
    /**
     * Creates a TempMessage with the current timestamp and unread status.
     */
    public static TempMessage create(String id, String subject, String from) {
        return new TempMessage(id, subject, from, LocalDateTime.now(), false);
    }

    /**
     * Returns the sender's domain (e.g., "gmail.com" from "user@gmail.com").
     */
    public String getSenderDomain() {
        if (from != null && from.contains("@")) {
            return from.split("@")[1];
        }
        return "unknown";
    }

    /**
     * Returns a shortened version of the subject for display purposes.
     */
    public String getShortSubject(int maxLength) {
        if (subject == null || subject.length() <= maxLength) {
            return subject != null ? subject : "No Subject";
        }
        return subject.substring(0, maxLength - 3) + "...";
    }
}
