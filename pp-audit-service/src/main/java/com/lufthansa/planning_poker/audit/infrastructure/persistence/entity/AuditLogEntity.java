package com.lufthansa.planning_poker.audit.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_user", columnList = "user_id")
})
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class AuditLogEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 36)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditAction action;

    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "source_service", length = 50)
    private String sourceService;

    public enum AuditAction {
        CREATE, UPDATE, DELETE, START, FINISH, JOIN, LEAVE, VOTE
    }
}

