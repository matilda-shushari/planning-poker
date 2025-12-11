package com.lufthansa.planning_poker.audit.infrastructure.persistence.repository;

import com.lufthansa.planning_poker.audit.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaAuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    Page<AuditLogEntity> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    Page<AuditLogEntity> findByEntityType(String entityType, Pageable pageable);

    Page<AuditLogEntity> findByUserId(String userId, Pageable pageable);

    @Query("SELECT a FROM AuditLogEntity a WHERE a.timestamp BETWEEN :from AND :to ORDER BY a.timestamp DESC")
    Page<AuditLogEntity> findByTimestampBetween(
        @Param("from") Instant from,
        @Param("to") Instant to,
        Pageable pageable
    );

    @Query("SELECT a FROM AuditLogEntity a WHERE " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:action IS NULL OR a.action = :action) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLogEntity> findWithFilters(
        @Param("entityType") String entityType,
        @Param("userId") String userId,
        @Param("action") AuditLogEntity.AuditAction action,
        Pageable pageable
    );

    List<AuditLogEntity> findTop100ByOrderByTimestampDesc();
}

