package com.tradeguard.surveillance;

import java.time.Instant;

public record CaseResponse(long id, long alertId, String proposedBy, CaseStatus status,
                           String reviewedBy, String reviewNote, Instant createdAt,
                           Instant reviewedAt) {

    public static CaseResponse from(SurveillanceCase surveillanceCase) {
        return new CaseResponse(surveillanceCase.getId(), surveillanceCase.getAlertId(),
                surveillanceCase.getProposedBy(), surveillanceCase.getStatus(),
                surveillanceCase.getReviewedBy(), surveillanceCase.getReviewNote(),
                surveillanceCase.getCreatedAt(), surveillanceCase.getReviewedAt());
    }
}
