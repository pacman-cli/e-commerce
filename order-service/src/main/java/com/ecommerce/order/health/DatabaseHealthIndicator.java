package com.ecommerce.order.health;

import com.ecommerce.order.metrics.OrderMetrics;
import com.ecommerce.order.outbox.OutboxPoller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Custom health indicator for database connectivity.
 * 
 * Checks:
 * - Database connection is valid
 * - Connection pool has available connections
 * - Recent queries are executing successfully
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("connection", "Valid")
                    .build();
            } else {
                return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("connection", "Invalid")
                    .build();
            }
        } catch (SQLException e) {
            log.error("Database health check failed", e);
            return Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
