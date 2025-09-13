package org.fergs.objects;

import java.time.LocalDateTime;

/**
 * Represents a temporary email address with its associated metadata.
 * This record holds all the information needed to manage a temporary email account.
 *
 * @param id        The unique identifier for the email account
 * @param address   The full email address (e.g., user@domain.com)
 * @param password  The password for the email account
 * @param token     JWT authentication token for API access
 * @param createdAt The timestamp when the email was created
 *
 * @Author Fergs32
 */
public record TempEmail(
    String id,
    String address,
    String password,
    String token,
    LocalDateTime createdAt
) {
    /**
     * Creates a TempEmail with the current timestamp.
     */
    public static TempEmail create(String id, String address, String password, String token) {
        return new TempEmail(id, address, password, token, LocalDateTime.now());
    }

    /**
     * Returns just the username part of the email address.
     */
    public String getUsername() {
        return address.split("@")[0];
    }

    /**
     * Returns just the domain part of the email address.
     */
    public String getDomain() {
        return address.split("@")[1];
    }
}
