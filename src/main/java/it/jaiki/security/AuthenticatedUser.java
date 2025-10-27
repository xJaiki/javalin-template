package it.jaiki.security;

/**
 * Represents the authenticated principal stored in the HTTP session.
 */
public record AuthenticatedUser(long id, String username, Role role) {
}
