package it.jaiki.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response returned by the login endpoint containing a JWT and the user summary.
 */
public final class LoginResponse {

    private final String token;
    private final UserResponse user;

    public LoginResponse(
        @JsonProperty("token") String token,
        @JsonProperty("user") UserResponse user
    ) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public UserResponse getUser() {
        return user;
    }
}
