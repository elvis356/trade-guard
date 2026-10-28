package com.tradeguard.surveillance;

import com.tradeguard.instrument.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final SurveillanceService surveillance;

    public AlertController(SurveillanceService surveillance) {
        this.surveillance = surveillance;
    }

    @GetMapping
    public ResponseEntity<PageResponse<AlertResponse>> findAlerts(
            @PageableDefault(size = 20, sort = "occurredAt",
                    direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(surveillance.findAlerts(pageable)));
    }

    @PostMapping("/{id}/case")
    public ResponseEntity<CaseResponse> proposeCase(
            Authentication authentication, @PathVariable long id) {
        return ResponseEntity.ok(surveillance.proposeCase(id, authentication.getName()));
    }
}
