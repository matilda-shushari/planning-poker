package com.lufthansa.planning_poker.audit.application.service;

import com.lufthansa.planning_poker.audit.application.dto.response.AuditLogResponse;
import com.lufthansa.planning_poker.audit.infrastructure.persistence.entity.AuditLogEntity;
import com.lufthansa.planning_poker.audit.infrastructure.persistence.repository.JpaAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for querying audit logs.
 * <p>
 * Provides read-only access to audit trail data for administrators.
 * Supports filtering by entity type, user, and action type.
 * </p>
 *
 * @author Matilda Dervishaj
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuditService {

    private final JpaAuditLogRepository auditLogRepository;

    /**
     * Retrieves all audit logs with pagination.
     *
     * @param pageable pagination parameters
     * @return paginated audit log entries
     */
    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable)
            .map(this::toResponse);
    }

    public Page<AuditLogResponse> getAuditLogsByEntity(String entityType, String entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable)
            .map(this::toResponse);
    }

    public Page<AuditLogResponse> getAuditLogsByEntityType(String entityType, Pageable pageable) {
        return auditLogRepository.findByEntityType(entityType, pageable)
            .map(this::toResponse);
    }

    public Page<AuditLogResponse> getAuditLogsByUser(String userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable)
            .map(this::toResponse);
    }

    /**
     * Searches audit logs with multiple filter criteria.
     *
     * @param entityType filter by entity type (ROOM, STORY, VOTE)
     * @param userId     filter by user who triggered the action
     * @param action     filter by action type (CREATE, UPDATE, DELETE, VOTE)
     * @param pageable   pagination parameters
     * @return paginated filtered audit log entries
     */
    public Page<AuditLogResponse> searchAuditLogs(
            String entityType,
            String userId,
            String action,
            Pageable pageable) {
        AuditLogEntity.AuditAction auditAction = action != null 
            ? AuditLogEntity.AuditAction.valueOf(action) 
            : null;
        return auditLogRepository.findWithFilters(entityType, userId, auditAction, pageable)
            .map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLogEntity entity) {
        return new AuditLogResponse(
            entity.getId(),
            entity.getEventId(),
            entity.getEventType(),
            entity.getEntityType(),
            entity.getEntityId(),
            entity.getAction().name(),
            entity.getEventData(),
            entity.getUserId(),
            entity.getUserName(),
            entity.getTimestamp(),
            entity.getSourceService()
        );
    }
}

