package it.jaiki.controller;

import it.jaiki.model.request.UserLoginRequest;
import it.jaiki.model.request.UserRegistrationRequest;
import it.jaiki.model.response.LoginResponse;
import it.jaiki.model.response.UserResponse;
import it.jaiki.security.AuthenticatedUser;
import it.jaiki.security.JwtUtil;
import it.jaiki.security.Role;
import it.jaiki.security.SecurityUtils;
import it.jaiki.service.AuthService;
import it.jaiki.service.AuthService.AuthenticationException;
import it.jaiki.service.AuthService.DuplicateUserException;
import it.jaiki.service.AuthService.ValidationException;
import it.jaiki.repository.UserRepository;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

/**
 * Exposes HTTP routes for authentication workflows.
 */
public final class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/auth/register", this::register, Role.PUBLIC);
        app.post("/api/auth/login", this::login, Role.PUBLIC);
        app.post("/api/auth/logout", this::logout, Role.USER, Role.ADMIN);
        app.get("/api/auth/me", this::currentUser, Role.USER, Role.ADMIN);

        app.exception(ValidationException.class, this::handleValidationException);
        app.exception(DuplicateUserException.class, this::handleDuplicateUserException);
        app.exception(AuthenticationException.class, this::handleAuthenticationException);
        app.exception(UserRepository.RepositoryException.class, this::handleRepositoryException);
    }

    @OpenApi(
        path = "/api/auth/register",
        methods = {HttpMethod.POST},
        summary = "Register a new account",
        tags = {"Authentication"},
        requestBody = @OpenApiRequestBody(content = {@OpenApiContent(from = UserRegistrationRequest.class)}),
        responses = {
            @OpenApiResponse(status = "201", description = "User registered", content = {@OpenApiContent(from = UserResponse.class)}),
            @OpenApiResponse(status = "400", description = "Validation error", content = {@OpenApiContent(from = ErrorResponse.class)}),
            @OpenApiResponse(status = "409", description = "Duplicate username", content = {@OpenApiContent(from = ErrorResponse.class)})
        }
    )
    private void register(Context ctx) {
        UserRegistrationRequest request = ctx.bodyAsClass(UserRegistrationRequest.class);
        UserResponse user = authService.register(request);
        SecurityUtils.storeCurrentUser(ctx, toAuthenticatedUser(user));
        ctx.status(HttpStatus.CREATED).json(user);
    }

    @OpenApi(
        path = "/api/auth/login",
        methods = {HttpMethod.POST},
        summary = "Authenticate an existing user",
        tags = {"Authentication"},
        requestBody = @OpenApiRequestBody(content = {@OpenApiContent(from = UserLoginRequest.class)}),
        responses = {
            @OpenApiResponse(status = "200", description = "Login successful", content = {@OpenApiContent(from = LoginResponse.class)}),
            @OpenApiResponse(status = "400", description = "Validation error", content = {@OpenApiContent(from = ErrorResponse.class)}),
            @OpenApiResponse(status = "401", description = "Invalid credentials", content = {@OpenApiContent(from = ErrorResponse.class)})
        }
    )
    private void login(Context ctx) {
        UserLoginRequest request = ctx.bodyAsClass(UserLoginRequest.class);
        UserResponse user = authService.login(request);

        // generate a JWT and return it alongside the user info
        String token = null;
        try {
            token = JwtUtil.generateToken(toAuthenticatedUser(user));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(toErrorResponse(e));
            return;
        }

        // also store in session for compatibility with session based flows
        SecurityUtils.storeCurrentUser(ctx, toAuthenticatedUser(user));

        ctx.json(new LoginResponse(token, user));
    }

    @OpenApi(
        path = "/api/auth/logout",
        methods = {HttpMethod.POST},
        summary = "Log out the current user",
        tags = {"Authentication"},
        responses = {
            @OpenApiResponse(status = "204", description = "Logged out")
        }
    )
    private void logout(Context ctx) {
        SecurityUtils.clearCurrentUser(ctx);
        ctx.status(HttpStatus.NO_CONTENT);
    }

    @OpenApi(
        path = "/api/auth/me",
        methods = {HttpMethod.GET},
        summary = "Retrieve the current user",
        tags = {"Authentication"},
        responses = {
            @OpenApiResponse(status = "200", description = "Current user", content = {@OpenApiContent(from = UserResponse.class)}),
            @OpenApiResponse(status = "404", description = "User not found", content = {@OpenApiContent(from = ErrorResponse.class)})
        }
    )
    private void currentUser(Context ctx) {
        AuthenticatedUser sessionUser = SecurityUtils.getCurrentUser(ctx);
        if (sessionUser == null) {
            ctx.status(HttpStatus.NOT_FOUND).json(new ErrorResponse("User not found"));
            return;
        }

        Optional<UserResponse> user = authService.findUser(sessionUser.id());
        if (user.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).json(new ErrorResponse("User not found"));
            SecurityUtils.clearCurrentUser(ctx);
            return;
        }

        ctx.json(user.get());
    }

    private void handleValidationException(ValidationException exception, Context ctx) {
        ctx.status(HttpStatus.BAD_REQUEST).json(toErrorResponse(exception));
    }

    private void handleDuplicateUserException(DuplicateUserException exception, Context ctx) {
        ctx.status(HttpStatus.CONFLICT).json(toErrorResponse(exception));
    }

    private void handleAuthenticationException(AuthenticationException exception, Context ctx) {
        ctx.status(HttpStatus.UNAUTHORIZED).json(toErrorResponse(exception));
    }

    private void handleRepositoryException(UserRepository.RepositoryException exception, Context ctx) {
        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(toErrorResponse(exception));
    }

    private AuthenticatedUser toAuthenticatedUser(UserResponse user) {
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getRole());
    }

    private ErrorResponse toErrorResponse(Exception exception) {
        return new ErrorResponse(exception.getMessage(), stackTrace(exception));
    }

    private String stackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
