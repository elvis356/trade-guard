package com.tradeguard.health;

import org.springframework.dao.DataAccessException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbc;
    private final RedisConnectionFactory redisConnectionFactory;

    public HealthController(JdbcTemplate jdbc, RedisConnectionFactory redisConnectionFactory) {
        this.jdbc = jdbc;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> healthPage() {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", java.nio.charset.StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noStore())
                .header("Content-Security-Policy",
                        "default-src 'none'; style-src 'unsafe-inline'; img-src 'self'; "
                                + "base-uri 'none'; form-action 'none'; frame-ancestors 'none'")
                .body(new ClassPathResource("static/health.html"));
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> healthStatus() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping(value = "/readiness", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, String> checks = new LinkedHashMap<>();
        checks.put("api", "UP");
        checks.put("database", checkDatabase());
        checks.put("redis", checkRedis());

        boolean ready = checks.values().stream().allMatch("UP"::equals);
        return ResponseEntity.status(ready ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", ready ? "UP" : "DOWN", "checks", checks));
    }

    private String checkDatabase() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (DataAccessException ex) {
            return "DOWN";
        }
    }

    private String checkRedis() {
        try (var connection = redisConnectionFactory.getConnection()) {
            return "PONG".equalsIgnoreCase(connection.ping()) ? "UP" : "DOWN";
        } catch (DataAccessException ex) {
            return "DOWN";
        }
    }
}
