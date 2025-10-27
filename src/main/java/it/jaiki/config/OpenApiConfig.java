package it.jaiki.config;

import io.javalin.config.JavalinConfig;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;

/**
 * Configures OpenAPI generation and documentation tooling.
 */
public final class OpenApiConfig {

    private OpenApiConfig() {
    }

    public static void register(JavalinConfig config) {
        config.registerPlugin(new OpenApiPlugin(openApiConfig ->
            openApiConfig
                .withDocumentationPath("/openapi")
                .withDefinitionConfiguration((version, definition) ->
                    definition.withInfo(info -> info
                        .title("Product Service API")
                        .summary("CRUD example built with Javalin")
                        .version("1.0.0"))
                        .withServer(server -> server
                            .url("http://localhost:{port}/{basePath}")
                            .description("Local development server")
                            .variable("port", "Server port", "8080", "8080", "7000")
                            .variable("basePath", "API base path", "", "", "api"))
                        // Add Bearer auth security scheme so Swagger UI can use the Authorize button
                        .withSecurity(security -> security.withBearerAuth("bearerAuth"))
                )
        ));

        config.registerPlugin(new SwaggerPlugin(swaggerConfig -> {
            swaggerConfig.setUiPath("/swagger");
            swaggerConfig.setDocumentationPath("/openapi?version=default");
            swaggerConfig.setTitle("Product Service API");
        }));
    }
}
