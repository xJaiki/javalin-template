package it.jaiki.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Simple DTO for error responses.
 */
public final class ErrorResponse {

    private final String message;
    private final String details;

    public ErrorResponse(@JsonProperty("message") String message) {
        this(message, null);
    }

    @JsonCreator
    public ErrorResponse(
        @JsonProperty("message") String message,
        @JsonProperty("details") String details
    ) {
        this.message = message;
        this.details = details;
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }
}
