package it.jaiki.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.time.Duration;

public final class DatabaseConfig {

    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/javalin-example";
    private static final String DEFAULT_USERNAME = "postgres";
    private static final String DEFAULT_PASSWORD = "";

    private DatabaseConfig() {
    }

    public static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getEnv("DB_URL", DEFAULT_URL));
        config.setUsername(getEnv("DB_USER", DEFAULT_USERNAME));
        config.setPassword(getEnv("DB_PASSWORD", DEFAULT_PASSWORD));
        config.setDriverClassName("org.postgresql.Driver");
        config.setPoolName("javalin-hikari-pool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(Duration.ofSeconds(30).toMillis());
        config.setLeakDetectionThreshold(Duration.ofSeconds(30).toMillis());
        return new HikariDataSource(config);
    }

    public static void runMigrations(DataSource dataSource) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load()
            .migrate();
    }

    private static String getEnv(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
