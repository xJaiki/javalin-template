package it.jaiki.repository;

import it.jaiki.model.User;
import it.jaiki.security.Role;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Provides JDBC access to the users table.
 */
public class UserRepository {

    private final DataSource dataSource;

    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, role, created_at FROM users WHERE username = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Unable to load user with username " + username, exception);
        }
    }

    public Optional<User> findById(long id) {
        String sql = "SELECT id, username, password_hash, role, created_at FROM users WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Unable to load user with id " + id, exception);
        }
    }

    public User insert(String username, String passwordHash, Role role) {
        String sql = "INSERT INTO users(username, password_hash, role) VALUES (?, ?, ?) RETURNING id, username, password_hash, role, created_at";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, role.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
                throw new RepositoryException("Insert did not return the created user");
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Unable to create user", exception);
        }
    }

    private User mapRow(ResultSet resultSet) throws SQLException {
        long id = resultSet.getLong("id");
        String username = resultSet.getString("username");
        String passwordHash = resultSet.getString("password_hash");
        Role role = Role.valueOf(resultSet.getString("role"));
        OffsetDateTime createdAt = toOffsetDateTime(resultSet, "created_at");
        return new User(id, username, passwordHash, role, createdAt);
    }

    private OffsetDateTime toOffsetDateTime(ResultSet resultSet, String column) throws SQLException {
        OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
        if (value != null) {
            return value;
        }
        return resultSet.getTimestamp(column).toInstant().atOffset(ZoneOffset.UTC);
    }

    public static class RepositoryException extends RuntimeException {
        public RepositoryException(String message) {
            super(message);
        }

        public RepositoryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
