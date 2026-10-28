package com.tradeguard.audit;

import java.time.Instant;
import java.util.List;

public record AuditVerification(boolean valid, long entriesChecked, Long brokenEntryId,
                                String reason, Instant verifiedAt) {

    public static AuditVerification valid(long entriesChecked, Instant verifiedAt) {
        return new AuditVerification(true, entriesChecked, null, null, verifiedAt);
    }

    public static AuditVerification invalid(long entriesChecked, Long brokenEntryId,
                                            String reason, Instant verifiedAt) {
        return new AuditVerification(false, entriesChecked, brokenEntryId, reason, verifiedAt);
    }
}
