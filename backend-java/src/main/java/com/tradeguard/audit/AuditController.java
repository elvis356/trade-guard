package com.tradeguard.audit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;
    private final ReconciliationService reconciliationService;

    public AuditController(AuditService auditService,
                           ReconciliationService reconciliationService) {
        this.auditService = auditService;
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/verify")
    public ResponseEntity<AuditVerification> verify() {
        AuditVerification result = auditService.verify();
        return ResponseEntity.status(result.valid()
                ? org.springframework.http.HttpStatus.OK
                : org.springframework.http.HttpStatus.CONFLICT).body(result);
    }

    @GetMapping("/reconcile")
    public ResponseEntity<ReconciliationReport> reconcile() {
        ReconciliationReport result = reconciliationService.reconcile();
        return ResponseEntity.status(result.balanced()
                ? org.springframework.http.HttpStatus.OK
                : org.springframework.http.HttpStatus.CONFLICT).body(result);
    }
}
