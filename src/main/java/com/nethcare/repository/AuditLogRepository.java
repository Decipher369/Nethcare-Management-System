package com.nethcare.repository;

import com.nethcare.model.AuditAction;
import com.nethcare.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * The audit trail.
 *
 * The only write path is AuditService.record(...), which appends. There are
 * deliberately no derived update queries here — nothing in the application
 * amends a row it has already written.
 *
 * JpaRepository does hand out delete() and deleteAll(), and it is worth being
 * straight about that: they would work against this table. The guarantees that
 * actually hold, in order of how much they are worth:
 *
 *   - the MySQL trigger in schema/audit_immutable.sql, which refuses UPDATE
 *     and DELETE however they are issued — this is the one that counts
 *   - @Immutable on the entity, which stops Hibernate issuing an UPDATE
 *   - convention, and convention is not a security control
 *
 * The trigger is the real answer. @Immutable alone is not: measured on this
 * schema, it blocks the update but repository.deleteById() still removes the
 * row. Until the trigger has been applied, treat this table as append-only by
 * agreement rather than by enforcement, and say so rather than claiming
 * otherwise.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByOccurredAtDescIdDesc();

    Optional<AuditLog> findTopByOrderByIdDesc();

    List<AuditLog> findByActorOrderByOccurredAtDescIdDesc(String actor);

    List<AuditLog> findByEntityNameOrderByOccurredAtDescIdDesc(String entityName);

    List<AuditLog> findByActionOrderByOccurredAtDescIdDesc(AuditAction action);

    List<AuditLog> findByOccurredAtBetweenOrderByOccurredAtDescIdDesc(
            LocalDateTime from, LocalDateTime to);

    /** The audit screen's filter. Any argument may be null to leave it out. */
    @Query("""
            select a from AuditLog a
            where (:actor is null or a.actor = :actor)
              and (:entityName is null or a.entityName = :entityName)
              and (:action is null or a.action = :action)
              and (:from is null or a.occurredAt >= :from)
              and (:to is null or a.occurredAt <= :to)
            order by a.occurredAt desc, a.id desc
            """)
    List<AuditLog> search(@Param("actor") String actor,
                          @Param("entityName") String entityName,
                          @Param("action") AuditAction action,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to);
}
