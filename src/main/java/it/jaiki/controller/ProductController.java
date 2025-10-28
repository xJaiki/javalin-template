package it.jaiki.controller;

import it.jaiki.model.Product;
import it.jaiki.model.request.ProductCreateRequest;
import it.jaiki.model.request.ProductUpdateRequest;
import it.jaiki.repository.ProductRepository;
import it.jaiki.security.Role;
import it.jaiki.service.ProductService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

/**
 * Exposes HTTP routes for product management.
 */
public final class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    public void registerRoutes(Javalin app) {
    // Public reads
    app.get("/api/products", this::listProducts, Role.PUBLIC);
    app.get("/api/products/{id}", this::getProduct, Role.PUBLIC);
    // Authenticated users and admins can create/update
    app.post("/api/products", this::createProduct, Role.USER, Role.ADMIN);
    app.put("/api/products/{id}", this::updateProduct, Role.USER, Role.ADMIN);
    // Only admins can delete
    app.delete("/api/products/{id}", this::deleteProduct, Role.ADMIN);
        app.exception(ProductService.ValidationException.class, this::handleValidationException);
        app.exception(ProductRepository.RepositoryException.class, this::handleRepositoryException);
    }

    @OpenApi(
        path = "/api/products",
        methods = {HttpMethod.GET},
        summary = "List all products",
        tags = {"Products"},
        responses = {
            @OpenApiResponse(
                status = "200",
                description = "Collection of persisted products",
                content = {@OpenApiContent(from = Product[].class)}
            )
        }
    )
    public void listProducts(Context ctx) {
        ctx.json(productService.listProducts());
    }

    @OpenApi(
        path = "/api/products/{id}",
        methods = {HttpMethod.GET},
        summary = "Retrieve a product by id",
        tags = {"Products"},
        pathParams = {
            @OpenApiParam(name = "id", type = Long.class, description = "Product identifier")
        },
        responses = {
            @OpenApiResponse(status = "200", content = {@OpenApiContent(from = Product.class)}),
            @OpenApiResponse(status = "404", description = "Product not found", content = {@OpenApiContent(from = ErrorResponse.class)})
        }
    )
    public void getProduct(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Optional<Product> product = productService.findProduct(id);
        if (product.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).json(new ErrorResponse("Product %d not found".formatted(id)));
            return;
        }
        ctx.json(product.get());
    }

    @OpenApi(
        path = "/api/products",
        methods = {HttpMethod.POST},
        summary = "Create a new product",
        tags = {"Products"},
        requestBody = @OpenApiRequestBody(content = {@OpenApiContent(from = ProductCreateRequest.class)}),
        responses = {
            @OpenApiResponse(status = "201", content = {@OpenApiContent(from = Product.class)}),
            @OpenApiResponse(status = "400", description = "Validation error", content = {@OpenApiContent(from = ErrorResponse.class)})
        }
    )
    public void createProduct(Context ctx) {
        ProductCreateRequest request = ctx.bodyAsClass(ProductCreateRequest.class);
        Product created = productService.createProduct(request);
        ctx.status(HttpStatus.CREATED).json(created);
    }

    @OpenApi(
        path = "/api/products/{id}",
        methods = {HttpMethod.PUT},
        summary = "Update an existing product",
        tags = {"Products"},
        pathParams = {
            @OpenApiParam(name = "id", type = Long.class, description = "Product identifier")
        },
        requestBody = @OpenApiRequestBody(content = {@OpenApiContent(from = ProductUpdateRequest.class)}),
        responses = {
            @OpenApiResponse(status = "200", content = {@OpenApiContent(from = Product.class)}),
            @OpenApiResponse(status = "400", description = "Validation error", content = {@OpenApiContent(from = ErrorResponse.class)}),
            @OpenApiResponse(status = "404", description = "Product not found", content = {@OpenApiContent(from = ErrorResponse.class)})
        }
    )
    public void updateProduct(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        ProductUpdateRequest request = ctx.bodyAsClass(ProductUpdateRequest.class);
        Optional<Product> updated = productService.updateProduct(id, request);
        if (updated.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).json(new ErrorResponse("Product %d not found".formatted(id)));
            return;
        }
        ctx.json(updated.get());
    }

    @OpenApi(
        path = "/api/products/{id}",
        methods = {HttpMethod.DELETE},
        summary = "Delete a product",
        tags = {"Products"},
        pathParams = {
            @OpenApiParam(name = "id", type = Long.class, description = "Product identifier")
        },
        responses = {
            @OpenApiResponse(status = "204", description = "Product deleted"),
            @OpenApiResponse(status = "404", description = "Product not found", content = {@OpenApiContent(from = ErrorResponse.class)})
        }
    )
    public void deleteProduct(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        boolean deleted = productService.deleteProduct(id);
        if (!deleted) {
            ctx.status(HttpStatus.NOT_FOUND).json(new ErrorResponse("Product %d not found".formatted(id)));
            return;
        }
        ctx.status(HttpStatus.NO_CONTENT);
    }

    private void handleValidationException(ProductService.ValidationException exception, Context ctx) {
        ctx.status(HttpStatus.BAD_REQUEST).json(toErrorResponse(exception));
    }

    private void handleRepositoryException(ProductRepository.RepositoryException exception, Context ctx) {
        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(toErrorResponse(exception));
    }

    private ErrorResponse toErrorResponse(Exception exception) {
        if (it.jaiki.config.AppConfig.shouldExposeErrorDetails()) {
            return new ErrorResponse(exception.getMessage(), stackTrace(exception));
        }
        return new ErrorResponse(exception.getMessage());
    }

    private String stackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
