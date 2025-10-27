package it.jaiki.model;

import it.jaiki.security.Role;

import java.time.OffsetDateTime;

/**
 * Represents an authenticated user persisted in the system.
 */
public class User {

    private final long id;
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final OffsetDateTime createdAt;

    public User(long id, String username, String passwordHash, Role role, OffsetDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
