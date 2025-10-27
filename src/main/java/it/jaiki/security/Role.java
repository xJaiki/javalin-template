package it.jaiki.security;

import io.javalin.security.RouteRole;

/**
 * Represents authorization roles for the application.
 */
public enum Role implements RouteRole {
    PUBLIC,
    USER,
    ADMIN
}
