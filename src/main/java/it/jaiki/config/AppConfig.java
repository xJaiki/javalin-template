package it.jaiki.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Small application configuration helper. Loads a local `.env` (if present) and
 * exposes convenience methods for environment-aware behavior.
 */
public final class AppConfig {

    private static final Path DOTENV_PATH = Path.of(".env");
    private static final Map<String, String> local = new HashMap<>();

    private AppConfig() {
    }

    public static void load() {
        if (Files.exists(DOTENV_PATH)) {
            try {
                List<String> lines = Files.readAllLines(DOTENV_PATH, StandardCharsets.UTF_8);
                for (String raw : lines) {
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int idx = line.indexOf('=');
                    if (idx <= 0) continue;
                    String k = line.substring(0, idx).trim();
                    String v = line.substring(idx + 1).trim();
                    // strip optional surrounding quotes
                    if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                        v = v.substring(1, v.length() - 1);
                    }
                    local.put(k, v);
                }
            } catch (IOException e) {
                // swallowing is ok; we fallback to env vars
            }
        }
    }

    public static String get(String key, String fallback) {
        String v = local.get(key);
        if (v != null && !v.isBlank()) return v;
        String env = System.getenv(key);
        return env == null || env.isBlank() ? fallback : env;
    }

    public static boolean isDev() {
        String env = get("APP_ENV", "development");
        return env.equalsIgnoreCase("development") || env.equalsIgnoreCase("dev");
    }

    public static boolean isDebug() {
        String v = get("DEBUG", "false");
        return v.equalsIgnoreCase("1") || v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes");
    }

    public static boolean shouldExposeErrorDetails() {
        return isDev() || isDebug();
    }
}
