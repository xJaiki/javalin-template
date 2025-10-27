package it.jaiki.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Small utility to issue and verify JWT tokens.
 */
public final class JwtUtil {

    private static final String DEFAULT_SECRET = "change-me-at-least-32-chars";
    private static final long DEFAULT_EXPIRATION_HOURS = 24;

    private JwtUtil() {
    }

    private static String jwtSecret() {
        String env = System.getenv("DEFAULT_JWT_SECRET");
        return env == null || env.isBlank() ? DEFAULT_SECRET : env;
    }

    public static String generateToken(AuthenticatedUser user) {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret());
        Instant now = Instant.now();
        return JWT.create()
            .withIssuedAt(now)
            .withExpiresAt(now.plus(DEFAULT_EXPIRATION_HOURS, ChronoUnit.HOURS))
            .withClaim("id", user.id())
            .withClaim("username", user.username())
            .withClaim("role", user.role().name())
            .sign(algorithm);
    }

    public static AuthenticatedUser parseToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret());
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT jwt = verifier.verify(token);
            long id = jwt.getClaim("id").asLong();
            String username = jwt.getClaim("username").asString();
            String roleStr = jwt.getClaim("role").asString();
            Role role = Role.valueOf(roleStr);
            return new AuthenticatedUser(id, username, role);
        } catch (JWTVerificationException | IllegalArgumentException e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }
}
