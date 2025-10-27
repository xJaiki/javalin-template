package it.jaiki.security;

import io.javalin.http.Context;

/**
 * Session helper methods for working with authenticated users.
 */
public final class SecurityUtils {

    private static final String CURRENT_USER_SESSION_KEY = "current-user";

    private SecurityUtils() {
    }

    public static void storeCurrentUser(Context context, AuthenticatedUser user) {
        context.sessionAttribute(CURRENT_USER_SESSION_KEY, user);
    }

    public static void clearCurrentUser(Context context) {
        context.sessionAttribute(CURRENT_USER_SESSION_KEY, null);
    }

    public static AuthenticatedUser getCurrentUser(Context context) {
        return context.sessionAttribute(CURRENT_USER_SESSION_KEY);
    }
}
