package com.avanzada.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * For profile "railway": provides a DataSource using DATABASE_URL or SPRING_DATASOURCE_*.
 * Converts Railway's mysql:// URL to jdbc:mysql:// so the driver accepts it.
 */
@Configuration
@Profile("railway")
public class RailwayDataSourceConfig {

    private static final String MYSQL_PREFIX = "mysql://";
    private static final String JDBC_MYSQL_PREFIX = "jdbc:mysql://";

    @Bean
    public DataSource dataSource(
            @Value("${SPRING_DATASOURCE_URL:${DATABASE_URL:}}") String url,
            @Value("${SPRING_DATASOURCE_USERNAME:${MYSQL_USER:}}") String username,
            @Value("${SPRING_DATASOURCE_PASSWORD:${MYSQL_PASSWORD:}}") String password) {
        String jdbcUrl = url;
        boolean urlHadCredentials = false;
        if (url != null && url.startsWith(MYSQL_PREFIX)) {
            jdbcUrl = JDBC_MYSQL_PREFIX + url.substring(MYSQL_PREFIX.length());
            urlHadCredentials = true; // mysql://user:pass@host form has credentials in URL
        }
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            jdbcUrl = "jdbc:mysql://localhost:3306/railway";
        }
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        if (!urlHadCredentials && username != null && !username.isBlank()) {
            ds.setUsername(username);
            ds.setPassword(password != null ? password : "");
        }
        ds.setDriverClassName("org.mariadb.jdbc.Driver");
        ds.setMaximumPoolSize(5);
        return ds;
    }
}
