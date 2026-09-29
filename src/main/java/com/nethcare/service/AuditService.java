package com.nethcare.service;

import com.nethcare.model.AuditAction;
import com.nethcare.model.AuditLog;
import com.nethcare.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    public AuditService(AuditLogRepository audit) {
        this.audit = audit;
    }

    /**
     * Appends one entry. REQUIRES_NEW so the entry survives even when the
     * operation being audited rolls back — if a create fails halfway we still
     * want the attempt on record, which is the whole point of a trail.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog record(String actor, AuditAction action, String entityName,
                           String entityId, String oldValue, String newValue) {
        return record(actor, action, entityName, entityId, oldValue, newValue, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog record(String actor, AuditAction action, String entityName,
                           String entityId, String oldValue, String newValue, String note) {
        AuditLog row = new AuditLog();
        row.setOccurredAt(LocalDateTime.now());
        row.setActor(actor == null || actor.isBlank() ? "system" : actor);
        row.setAction(action);
        row.setEntityName(entityName);
        row.setEntityId(entityId);
        row.setOldValue(trim(oldValue));
        row.setNewValue(trim(newValue));
        row.setNote(trim(note));
        return audit.save(row);
    }

    /** The columns are 500 wide; a whole entity dumped in would be refused. */
    private String trim(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 500 ? value : value.substring(0, 497) + "...";
    }
}
