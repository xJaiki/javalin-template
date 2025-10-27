package it.jaiki.model.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload for authenticating an existing user.
 */
public final class UserLoginRequest {

    private final String username;
    private final String password;

    @JsonCreator
    public UserLoginRequest(
        @JsonProperty(value = "username", required = true) String username,
        @JsonProperty(value = "password", required = true) String password
    ) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
