package com.avanzada.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;

/**
 * For profile "render": provides a DataSource using DATABASE_URL from Render PostgreSQL.
 * Converts Render's postgres:// or postgresql:// URL to jdbc:postgresql://host:port/db.
 */
@Configuration
@Profile("render")
public class RenderDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(RenderDataSourceConfig.class);

    @Bean
    public DataSource dataSource(
            @Value("${DATABASE_URL:}") String databaseUrl,
            @Value("${SPRING_DATASOURCE_URL:}") String springUrl,
            @Value("${SPRING_DATASOURCE_USERNAME:}") String springUser,
            @Value("${SPRING_DATASOURCE_PASSWORD:}") String springPass) {

        HikariDataSource ds = new HikariDataSource();

        if (springUrl != null && !springUrl.isBlank()) {
            log.info("Render DataSource: using SPRING_DATASOURCE_URL");
            ds.setJdbcUrl(springUrl);
            ds.setUsername(springUser);
            ds.setPassword(springPass);
        } else if (databaseUrl != null && !databaseUrl.isBlank()) {
            log.info("Render DataSource: converting DATABASE_URL to JDBC");
            String normalized = databaseUrl;
            if (normalized.startsWith("postgres://")) {
                normalized = "postgresql://" + normalized.substring("postgres://".length());
            }
            URI uri = URI.create(normalized);
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath();
            ds.setJdbcUrl(jdbcUrl);
            log.info("Render DataSource: JDBC URL = {}", jdbcUrl);
            String userInfo = uri.getUserInfo();
            if (userInfo != null) {
                String[] parts = userInfo.split(":", 2);
                ds.setUsername(parts[0]);
                ds.setPassword(parts.length > 1 ? parts[1] : "");
            }
        } else {
            log.error("Render DataSource: DATABASE_URL is empty! Set it in Render environment variables.");
            throw new IllegalStateException("DATABASE_URL environment variable is required for the render profile");
        }

        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(3);
        return ds;
    }
}
