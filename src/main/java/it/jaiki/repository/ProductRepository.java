package it.jaiki.repository;

import it.jaiki.model.Product;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Performs raw JDBC operations against the products table.
 */
public class ProductRepository {

    private final DataSource dataSource;

    public ProductRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Product> findAll() {
        String sql = "SELECT id, name, price, created_at, updated_at FROM products ORDER BY id";
        List<Product> products = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                products.add(mapRow(resultSet));
            }
            return products;
        } catch (SQLException exception) {
            throw new RepositoryException("Unable to load products", exception);
        }
    }

    public Optional<Product> findById(long id) {
        String sql = "SELECT id, name, price, created_at, updated_at FROM products WHERE id = ?";
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
            throw new RepositoryException("Unable to load product with id " + id, exception);
        }
    }

    public Product insert(String name, BigDecimal price) {
        String sql = "INSERT INTO products(name, price) VALUES (?, ?) RETURNING id, created_at, updated_at";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setBigDecimal(2, price);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    long id = resultSet.getLong("id");
                    OffsetDateTime createdAt = toOffsetDateTime(resultSet, "created_at");
                    OffsetDateTime updatedAt = toOffsetDateTime(resultSet, "updated_at");
                    return new Product(id, name, price, createdAt, updatedAt);
                }
                throw new RepositoryException("Insert did not return generated columns");
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Unable to create product", exception);
        }
    }

    public Optional<Product> update(long id, String name, BigDecimal price) {
        String sql = "UPDATE products SET name = ?, price = ? WHERE id = ? RETURNING id, name, price, created_at, updated_at";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setBigDecimal(2, price);
            statement.setLong(3, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Unable to update product " + id, exception);
        }
    }

    public boolean delete(long id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new RepositoryException("Unable to delete product " + id, exception);
        }
    }

    private Product mapRow(ResultSet resultSet) throws SQLException {
        long id = resultSet.getLong("id");
        String name = resultSet.getString("name");
        BigDecimal price = resultSet.getBigDecimal("price");
        OffsetDateTime createdAt = toOffsetDateTime(resultSet, "created_at");
        OffsetDateTime updatedAt = toOffsetDateTime(resultSet, "updated_at");
        return new Product(id, name, price, createdAt, updatedAt);
    }

    private OffsetDateTime toOffsetDateTime(ResultSet resultSet, String column) throws SQLException {
        OffsetDateTime timestamp = resultSet.getObject(column, OffsetDateTime.class);
        if (timestamp != null) {
            return timestamp;
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
