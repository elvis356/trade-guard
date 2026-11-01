package com.tradeguard.health;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

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
}
