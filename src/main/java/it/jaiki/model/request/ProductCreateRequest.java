package it.jaiki.model.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Represents payload for creating a product.
 */
public final class ProductCreateRequest {

    private final String name;
    private final BigDecimal price;

    @JsonCreator
    public ProductCreateRequest(
        @JsonProperty(value = "name", required = true) String name,
        @JsonProperty(value = "price", required = true) BigDecimal price
    ) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
