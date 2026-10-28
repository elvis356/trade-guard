package com.tradeguard.audit;

import com.tradeguard.common.ApiError;
import com.tradeguard.config.MarketProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;

@RestController
@RequestMapping("/internal")
public class InternalEodController {

    private final EodService eodService;
    private final MarketProperties market;
    private final String expectedToken;

    public InternalEodController(EodService eodService, MarketProperties market,
                                 @Value("${tradeguard.eod.token:}") String expectedToken) {
        if (!expectedToken.isBlank()
                && expectedToken.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("EOD_TOKEN must be at least 32 bytes when configured.");
        }
        this.eodService = eodService;
        this.market = market;
        this.expectedToken = expectedToken;
    }

    @PostMapping("/eod")
    public ResponseEntity<?> run(
            @RequestHeader(name = "X-EOD-Token", required = false) String token,
            @RequestParam(required = false) LocalDate date,
            HttpServletRequest request) {
        if (!matchesToken(token)) {
            ApiError error = ApiError.of(HttpStatus.UNAUTHORIZED.value(),
                    HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                    "日終作業憑證無效。", request.getRequestURI());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        LocalDate businessDate = date == null ? LocalDate.now(market.getZone()) : date;
        EodRunResult result = eodService.run(businessDate);
        return ResponseEntity.status(result.completed()
                ? HttpStatus.OK : HttpStatus.CONFLICT).body(result);
    }

    private boolean matchesToken(String supplied) {
        return supplied != null && !expectedToken.isBlank()
                && MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }
}
