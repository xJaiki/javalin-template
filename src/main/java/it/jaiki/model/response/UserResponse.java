package it.jaiki.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.jaiki.security.Role;

import java.time.OffsetDateTime;

/**
 * Public representation of a user without sensitive information.
 */
public final class UserResponse {

    private final long id;
    private final String username;
    private final Role role;
    private final OffsetDateTime createdAt;

    public UserResponse(
        @JsonProperty("id") long id,
        @JsonProperty("username") String username,
        @JsonProperty("role") Role role,
        @JsonProperty("createdAt") OffsetDateTime createdAt
    ) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
