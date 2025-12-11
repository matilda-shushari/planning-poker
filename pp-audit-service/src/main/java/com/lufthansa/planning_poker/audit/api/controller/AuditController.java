package com.lufthansa.planning_poker.audit.api.controller;

import com.lufthansa.planning_poker.audit.application.dto.response.AuditLogResponse;
import com.lufthansa.planning_poker.audit.application.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Audit", description = "Audit log endpoints (Admin only)")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Get all audit logs", description = "Returns paginated audit logs (Admin only)")
    public Page<AuditLogResponse> getAuditLogs(
            @PageableDefault(size = 50, sort = "timestamp") Pageable pageable) {
        return auditService.getAuditLogs(pageable);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get audit logs for specific entity")
    public Page<AuditLogResponse> getAuditLogsByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @PageableDefault(size = 50) Pageable pageable) {
        return auditService.getAuditLogsByEntity(entityType, entityId, pageable);
    }

    @GetMapping("/entity-type/{entityType}")
    @Operation(summary = "Get audit logs by entity type")
    public Page<AuditLogResponse> getAuditLogsByEntityType(
            @PathVariable String entityType,
            @PageableDefault(size = 50) Pageable pageable) {
        return auditService.getAuditLogsByEntityType(entityType, pageable);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get audit logs by user")
    public Page<AuditLogResponse> getAuditLogsByUser(
            @PathVariable String userId,
            @PageableDefault(size = 50) Pageable pageable) {
        return auditService.getAuditLogsByUser(userId, pageable);
    }

    @GetMapping("/search")
    @Operation(summary = "Search audit logs with filters")
    public Page<AuditLogResponse> searchAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @PageableDefault(size = 50) Pageable pageable) {
        return auditService.searchAuditLogs(entityType, userId, action, pageable);
    }
}
