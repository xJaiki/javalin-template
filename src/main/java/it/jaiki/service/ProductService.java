package it.jaiki.service;

import it.jaiki.model.Product;
import it.jaiki.model.request.ProductCreateRequest;
import it.jaiki.model.request.ProductUpdateRequest;
import it.jaiki.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Encapsulates business rules around product manipulation.
 */
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> listProducts() {
        return repository.findAll();
    }

    public Optional<Product> findProduct(long id) {
        return repository.findById(id);
    }

    public Product createProduct(ProductCreateRequest request) {
        validateName(request.getName());
        validatePrice(request.getPrice());
        return repository.insert(request.getName().trim(), request.getPrice());
    }

    public Optional<Product> updateProduct(long id, ProductUpdateRequest request) {
        Optional<Product> existing = repository.findById(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        String name = request.getName().map(String::trim).orElse(existing.get().getName());
        BigDecimal price = request.getPrice().orElse(existing.get().getPrice());

        validateName(name);
        validatePrice(price);

        return repository.update(id, name, price);
    }

    public boolean deleteProduct(long id) {
        return repository.delete(id);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Product name is required");
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new ValidationException("Product price is required");
        }
        if (price.scale() > 2) {
            throw new ValidationException("Product price supports at most two decimal places");
        }
        if (price.signum() < 0) {
            throw new ValidationException("Product price must be positive");
        }
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
}
