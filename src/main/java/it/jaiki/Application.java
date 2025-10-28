package it.jaiki;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.zaxxer.hikari.HikariDataSource;
import it.jaiki.config.DatabaseConfig;
import it.jaiki.config.OpenApiConfig;
import it.jaiki.controller.AuthController;
import it.jaiki.controller.ProductController;
import it.jaiki.repository.ProductRepository;
import it.jaiki.repository.UserRepository;
import it.jaiki.security.AuthenticatedUser;
import it.jaiki.security.Role;
import it.jaiki.security.SecurityUtils;
import it.jaiki.security.JwtUtil;
import it.jaiki.service.AuthService;
import it.jaiki.service.ProductService;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.json.JavalinJackson;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstraps the Javalin application.
 */
public final class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private Application() {
    }

    public static void main(String[] args) {
        // Load local .env (if present) and app config early
        it.jaiki.config.AppConfig.load();

        HikariDataSource dataSource = DatabaseConfig.createDataSource();
        DatabaseConfig.runMigrations(dataSource);

        ProductRepository productRepository = new ProductRepository(dataSource);
        ProductService productService = new ProductService(productRepository);
        ProductController productController = new ProductController(productService);

        UserRepository userRepository = new UserRepository(dataSource);
        AuthService authService = new AuthService(userRepository);
        seedDefaultAdmin(authService);
        AuthController authController = new AuthController(authService);

        Javalin app = Javalin.create(Application::configureJavalin);
        registerSecurity(app);
        authController.registerRoutes(app);
        productController.registerRoutes(app);

        // Health and readiness endpoints
        app.get("/health", ctx -> ctx.json(Map.of("status", "UP")));
        app.get("/ready", ctx -> {
            try (var conn = dataSource.getConnection()) {
                ctx.json(Map.of("status", "READY"));
            } catch (Exception e) {
                ctx.status(503).json(Map.of("status", "NOT_READY", "error", e.getMessage()));
            }
        });

        int port = resolvePort();
        LOGGER.info("Starting server on port {}", port);
        app.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down application");
            app.stop();
            dataSource.close();
        }));
    }

    private static void configureJavalin(JavalinConfig config) {
        config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
            mapper.findAndRegisterModules();
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }));

        OpenApiConfig.register(config);
    }

    private static void registerSecurity(Javalin app) {
        app.beforeMatched(ctx -> {
            var permittedRoles = ctx.routeRoles();
            if (permittedRoles == null || permittedRoles.isEmpty() || permittedRoles.contains(Role.PUBLIC)) {
                return;
            }

            final AuthenticatedUser[] currentUserHolder = new AuthenticatedUser[1];
            currentUserHolder[0] = SecurityUtils.getCurrentUser(ctx);
            // Attempt to accept a Bearer token in Authorization header if session not present
            if (currentUserHolder[0] == null) {
                String authHeader = ctx.header("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring("Bearer ".length());
                    try {
                        currentUserHolder[0] = JwtUtil.parseToken(token);
                        // store parsed principal in session attribute for downstream handlers
                        ctx.sessionAttribute("current-user", currentUserHolder[0]);
                    } catch (RuntimeException e) {
                        throw new UnauthorizedResponse();
                    }
                }
            }
            if (currentUserHolder[0] == null) {
                throw new UnauthorizedResponse();
            }

            boolean authorized = permittedRoles.stream()
                .map(role -> (Role) role)
                .anyMatch(role -> role == currentUserHolder[0].role());

            if (!authorized) {
                throw new ForbiddenResponse();
            }

            ctx.attribute("currentUser", currentUserHolder[0]);
        });
    }

    private static void seedDefaultAdmin(AuthService authService) {
        String username = getEnv("DEFAULT_ADMIN_USERNAME", "admin");
        String password = getEnv("DEFAULT_ADMIN_PASSWORD", "password");

        try {
            authService.ensureAdminUser(username, password);
        } catch (AuthService.ValidationException exception) {
            LOGGER.error("Unable to seed admin user: {}", exception.getMessage(), exception);
        }
    }

    private static String getEnv(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int resolvePort() {
        String portValue = System.getenv("PORT");
        if (portValue == null || portValue.isBlank()) {
            return 7000;
        }
        try {
            return Integer.parseInt(portValue);
        } catch (NumberFormatException exception) {
            LOGGER.warn("Invalid PORT environment variable '{}', falling back to 7000", portValue);
            return 7000;
        }
    }
}
