package com.avanzada.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * When profile "railway" is active and DATABASE_URL (or spring.datasource.url) is in
 * Railway's format (mysql://user:pass@host:port/db), converts it to JDBC format
 * (jdbc:mysql://...) so that the MariaDB/MySQL driver accepts it.
 */
public class RailwayDatabaseUrlProcessor implements EnvironmentPostProcessor {

    private static final String RAILWAY_PROFILE = "railway";
    private static final String MYSQL_PREFIX = "mysql://";
    private static final String JDBC_MYSQL_PREFIX = "jdbc:mysql://";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(RAILWAY_PROFILE)) {
            return;
        }
        String url = environment.getProperty("SPRING_DATASOURCE_URL");
        if (url == null || url.isBlank()) {
            url = environment.getProperty("DATABASE_URL");
        }
        if (url == null || url.isBlank() || !url.startsWith(MYSQL_PREFIX)) {
            return;
        }
        String jdbcUrl = JDBC_MYSQL_PREFIX + url.substring(MYSQL_PREFIX.length());
        environment.getPropertySources().addFirst(
                new MapPropertySource("railwayDbUrl", Map.of("spring.datasource.url", jdbcUrl))
        );
    }
}
