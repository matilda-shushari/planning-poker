package com.lufthansa.planning_poker.audit.application.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    UUID eventId,
    String eventType,
    String entityType,
    String entityId,
    String action,
    String eventData,
    String userId,
    String userName,
    Instant timestamp,
    String sourceService
) {}

