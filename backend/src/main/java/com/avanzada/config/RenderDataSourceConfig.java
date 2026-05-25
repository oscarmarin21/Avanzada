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
 * Converts Render's postgres://user:pass@host:port/db to jdbc:postgresql://host:port/db.
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
            URI uri = URI.create(databaseUrl.replace("postgres://", "postgresql://"));
            String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();
            ds.setJdbcUrl(jdbcUrl);
            log.info("Render DataSource: JDBC URL = {}", jdbcUrl);
            String[] userInfo = uri.getUserInfo().split(":", 2);
            ds.setUsername(userInfo[0]);
            ds.setPassword(userInfo.length > 1 ? userInfo[1] : "");
        } else {
            log.warn("Render DataSource: DATABASE_URL is empty! Falling back to localhost.");
            ds.setJdbcUrl("jdbc:postgresql://localhost:5432/avanzada");
            ds.setUsername("avanzada");
            ds.setPassword("avanzada");
        }

        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(3);
        return ds;
    }
}
