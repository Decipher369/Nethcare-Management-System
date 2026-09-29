package com.nethcare.service;

import com.nethcare.model.AuditAction;
import com.nethcare.model.AuditLog;
import com.nethcare.repository.AuditLogRepository;
import com.nethcare.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.time.temporal.ChronoUnit;

/**
 * The one place audit rows are written.
 *
 * The client asked for a row on every create, update and delete in the system,
 * so the other modules will call record(...) from their own services. There is
 * no update or delete here on purpose — a trail that can be rewritten is not a
 * trail.
 */
@Service
public class AuditService {

    private final AuditLogRepository audit;
    private final UserRepository users;

    public AuditService(AuditLogRepository audit, UserRepository users) {
        this.audit = audit;
        this.users = users;
    }

    /**
     * Appends in the caller's transaction. If evidence cannot be recorded,
     * the business change is rolled back as required by UC-4.5.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog record(String actor, AuditAction action, String entityName,
                           String entityId, String oldValue, String newValue) {
        return record(actor, action, entityName, entityId, oldValue, newValue, null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog record(String actor, AuditAction action, String entityName,
                           String entityId, String oldValue, String newValue, String note) {
        AuditLog row = new AuditLog();
        row.setOccurredAt(LocalDateTime.now(java.time.Clock.systemUTC()).truncatedTo(ChronoUnit.MICROS));
        row.setActor(actor == null || actor.isBlank() ? "system" : actor);
        if (actor != null && !actor.isBlank()) {
            users.findByUsername(actor).ifPresent(user -> row.setActorId(user.getId()));
        }
        row.setAction(action);
        row.setEntityName(entityName);
        row.setEntityId(entityId);
        row.setOldValue(trim(oldValue));
        row.setNewValue(trim(newValue));
        row.setNote(trim(note));
        String previous = audit.findTopByOrderByIdDesc()
                .map(AuditLog::getRecordHash).orElse(null);
        row.setPreviousHash(previous);
        row.setRecordHash(hash(previous, row));
        return audit.save(row);
    }

    @Transactional(readOnly = true)
    public boolean verifyChain() {
        String previous = null;
        for (AuditLog row : audit.findAll().stream()
                .sorted(java.util.Comparator.comparing(AuditLog::getId)).toList()) {
            if (!java.util.Objects.equals(previous, row.getPreviousHash())
                    || !java.util.Objects.equals(hash(previous, row), row.getRecordHash())) {
                return false;
            }
            previous = row.getRecordHash();
        }
        return true;
    }

    private String hash(String previous, AuditLog row) {
        String canonical = String.join("|",
                value(previous), value(row.getOccurredAt()), value(row.getActorId()), value(row.getActor()),
                value(row.getAction()), value(row.getEntityName()), value(row.getEntityId()),
                value(row.getOldValue()), value(row.getNewValue()), value(row.getNote()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private String value(Object value) { return value == null ? "" : value.toString(); }

    /** The columns are 500 wide; a whole entity dumped in would be refused. */
    private String trim(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 500 ? value : value.substring(0, 497) + "...";
    }
}
