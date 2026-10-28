package com.tradeguard.surveillance;

import com.tradeguard.instrument.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final SurveillanceService surveillance;

    public CaseController(SurveillanceService surveillance) {
        this.surveillance = surveillance;
    }

    @GetMapping
    public ResponseEntity<PageResponse<CaseResponse>> findCases(
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(surveillance.findCases(pageable)));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<CaseResponse> review(
            Authentication authentication, @PathVariable long id,
            @Valid @RequestBody ReviewCaseRequest request) {
        return ResponseEntity.ok(surveillance.reviewCase(
                id, authentication.getName(), request));
    }
}
