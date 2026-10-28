package com.tradeguard.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;

@Service
public class AuditService {

    private static final long CHAIN_STATE_ID = 1L;
    private static final String GENESIS_HASH = "0".repeat(64);

    private final AuditEntryRepository entries;
    private final AuditChainStateRepository chainState;

    public AuditService(AuditEntryRepository entries, AuditChainStateRepository chainState) {
        this.entries = entries;
        this.chainState = chainState;
    }

    @Transactional
    public void record(Long userId, String actor, String action, String detail) {
        AuditChainState state = chainState.findByIdForUpdate(CHAIN_STATE_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Audit chain state row is missing."));
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        String previousHash = state.getLastHash();
        String hash = hash(previousHash, userId, actor, action, detail, createdAt);
        entries.saveAndFlush(new AuditEntry(userId, actor, action, detail,
                createdAt, previousHash, hash));
        state.setLastHash(hash);
    }

    @Transactional(readOnly = true)
    public AuditVerification verify() {
        List<AuditEntry> all = entries.findAllByOrderByIdAsc();
        String expectedPrevious = GENESIS_HASH;
        long checked = 0;
        for (AuditEntry entry : all) {
            if (!expectedPrevious.equals(entry.getPreviousHash())) {
                return AuditVerification.invalid(checked, entry.getId(),
                        "Previous hash does not match the preceding entry.", Instant.now());
            }
            String expectedHash = hash(expectedPrevious, entry.getUserId(), entry.getActor(),
                    entry.getAction(), entry.getDetail(), entry.getCreatedAt());
            if (!expectedHash.equals(entry.getRowHash())) {
                return AuditVerification.invalid(checked, entry.getId(),
                        "Entry hash does not match its stored content.", Instant.now());
            }
            expectedPrevious = entry.getRowHash();
            checked++;
        }

        String storedHead = chainState.findById(CHAIN_STATE_ID)
                .map(AuditChainState::getLastHash)
                .orElse(null);
        if (!expectedPrevious.equals(storedHead)) {
            return AuditVerification.invalid(checked,
                    all.isEmpty() ? null : all.get(all.size() - 1).getId(),
                    "Stored chain head does not match the final entry.", Instant.now());
        }
        return AuditVerification.valid(checked, Instant.now());
    }

    private String hash(String previousHash, Long userId, String actor, String action,
                        String detail, Instant createdAt) {
        String canonical = previousHash + "|" + (userId == null ? "" : userId)
                + "|" + actor + "|" + action + "|" + detail + "|" + createdAt;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime.", ex);
        }
    }
}
