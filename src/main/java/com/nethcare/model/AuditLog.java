package com.nethcare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * One line in the audit trail.
 *
 * The client asked for this table to be unchangeable by anyone, including the
 * admin. Two things enforce that, and the first one alone is NOT enough:
 *
 *   - The entity is @Immutable, so Hibernate ignores changes to a loaded row
 *     and never issues an UPDATE. Measured on this schema: loading a row,
 *     changing a field and saving leaves the column untouched. It does NOT
 *     stop a delete, though — repository.deleteById() still removes the row,
 *     because @Immutable governs field updates, not the delete path.
 *   - A MySQL trigger raises on UPDATE and DELETE. This is the half that
 *     actually holds, because it applies to anything that reaches the
 *     database — another app, a script in a terminal, a mistake in a console.
 *
 * The trigger is created by hand in schema/audit_immutable.sql rather than by
 * ddl-auto, which cannot express one. Until it is applied the table is
 * append-only by convention only, and the README says so.
 *
 * Extending BaseEntity would hand it setters for isActive, which is exactly
 * the soft-delete path this table must never take — so it stands on its own
 * with createdAt as a plain column.
 */
@Entity
@Table(name = "audit_log")
@Immutable
public class AuditLog {

    @Column(name = "id")
    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "actor", nullable = false, length = 50)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, columnDefinition = "VARCHAR(10)")
    private AuditAction action;

    @Column(name = "entity_name", nullable = false, length = 60)
    private String entityName;

    @Column(name = "entity_id", length = 40)
    private String entityId;

    @Column(name = "old_value", length = 500)
    private String oldValue;

    @Column(name = "new_value", length = 500)
    private String newValue;

    @Column(name = "note", length = 200)
    private String note;

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public LocalDateTime getOccurredAt() { return occurredAt; }

    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    public String getActor() { return actor; }

    public void setActor(String actor) { this.actor = actor; }

    public AuditAction getAction() { return action; }

    public void setAction(AuditAction action) { this.action = action; }

    public String getEntityName() { return entityName; }

    public void setEntityName(String entityName) { this.entityName = entityName; }

    public String getEntityId() { return entityId; }

    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getOldValue() { return oldValue; }

    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }

    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getNote() { return note; }

    public void setNote(String note) { this.note = note; }
}
