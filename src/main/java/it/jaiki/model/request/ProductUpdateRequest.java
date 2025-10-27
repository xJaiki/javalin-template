package it.jaiki.model.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Represents payload for updating an existing product.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ProductUpdateRequest {

    private final String name;
    private final BigDecimal price;

    @JsonCreator
    public ProductUpdateRequest(
        @JsonProperty("name") String name,
        @JsonProperty("price") BigDecimal price
    ) {
        this.name = name;
        this.price = price;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<BigDecimal> getPrice() {
        return Optional.ofNullable(price);
    }
}
