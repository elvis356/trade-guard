package com.tradeguard.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthReadinessTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final RedisConnectionFactory redisConnectionFactory =
            mock(RedisConnectionFactory.class);
    private final RedisConnection redisConnection = mock(RedisConnection.class);
    private final HealthController controller =
            new HealthController(jdbc, redisConnectionFactory);

    @BeforeEach
    void dependenciesAreReadyByDefault() {
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");
    }

    @Test
    void reportsReadyWhenDatabaseAndRedisRespond() {
        var response = controller.readiness();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals(Map.of("api", "UP", "database", "UP", "redis", "UP"),
                response.getBody().get("checks"));
    }

    @Test
    void reportsNotReadyWhenDatabaseIsUnavailable() {
        when(jdbc.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new DataAccessResourceFailureException("Database unavailable"));

        var response = controller.readiness();

        assertEquals(503, response.getStatusCode().value());
        assertEquals(Map.of("api", "UP", "database", "DOWN", "redis", "UP"),
                response.getBody().get("checks"));
    }

    @Test
    void reportsNotReadyWhenRedisIsUnavailable() {
        when(redisConnectionFactory.getConnection())
                .thenThrow(new RedisConnectionFailureException("Redis unavailable"));

        var response = controller.readiness();

        assertEquals(503, response.getStatusCode().value());
        assertEquals(Map.of("api", "UP", "database", "UP", "redis", "DOWN"),
                response.getBody().get("checks"));
    }
}
