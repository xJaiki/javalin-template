package it.jaiki.service;

import it.jaiki.model.User;
import it.jaiki.model.request.UserLoginRequest;
import it.jaiki.model.request.UserRegistrationRequest;
import it.jaiki.model.response.UserResponse;
import it.jaiki.repository.UserRepository;
import it.jaiki.security.Role;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Handles user registration and authentication flows.
 */
public class AuthService {

    private static final int BCRYPT_ROUNDS = 12;
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse register(UserRegistrationRequest request) {
        String username = normalizeUsername(request.getUsername());
        String password = request.getPassword();

        validateUsername(username);
        validatePassword(password);

        Optional<User> existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            throw new DuplicateUserException("Username '%s' is already taken".formatted(username));
        }

        String passwordHash = hashPassword(username, password);
        User user = userRepository.insert(username, passwordHash, Role.USER);
        return toResponse(user);
    }

    public UserResponse login(UserLoginRequest request) {
        String username = normalizeUsername(request.getUsername());
        String password = request.getPassword();

        validateUsername(username);
        validatePassword(password);

        Optional<User> existing = userRepository.findByUsername(username);
        if (existing.isEmpty()) {
            throw new AuthenticationException("Invalid credentials");
        }

        User user = existing.get();
        if (!isPasswordValid(username, password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid credentials");
        }

        return toResponse(user);
    }

    public Optional<UserResponse> findUser(long id) {
        return userRepository.findById(id).map(this::toResponse);
    }

    public void ensureAdminUser(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        validateUsername(normalizedUsername);
        validatePassword(password);

        Optional<User> existing = userRepository.findByUsername(normalizedUsername);
        if (existing.isPresent()) {
            if (existing.get().getRole() != Role.ADMIN) {
                LOGGER.warn("Admin seed skipped because username '{}' is already used by a non-admin account", normalizedUsername);
            } else {
                LOGGER.debug("Admin seed skipped because username '{}' already exists", normalizedUsername);
            }
            return;
        }

        String passwordHash = hashPassword(normalizedUsername, password);
        userRepository.insert(normalizedUsername, passwordHash, Role.ADMIN);
        LOGGER.info("Seeded default admin account '{}'", normalizedUsername);
    }

    private String normalizeUsername(String username) {
        return username == null ? null : username.trim();
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Username is required");
        }
        if (username.length() < 3) {
            throw new ValidationException("Username must be at least 3 characters long");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password is required");
        }
        if (password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long");
        }
    }

    private String hashPassword(String username, String password) {
        try {
            return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS));
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Failed to hash password for user {}", username, exception);
            throw new ValidationException("Password hashing failed", exception);
        }
    }

    private boolean isPasswordValid(String username, String rawPassword, String passwordHash) {
        try {
            return BCrypt.checkpw(rawPassword, passwordHash);
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Failed to validate password for user {}", username, exception);
            throw new AuthenticationException("Stored password hash is invalid", exception);
        }
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.getCreatedAt());
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }

        public ValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DuplicateUserException extends RuntimeException {
        public DuplicateUserException(String message) {
            super(message);
        }

        public DuplicateUserException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }

        public AuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
