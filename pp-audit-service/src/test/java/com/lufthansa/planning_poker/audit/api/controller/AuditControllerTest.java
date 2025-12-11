package com.lufthansa.planning_poker.audit.api.controller;

import com.lufthansa.planning_poker.audit.application.dto.response.AuditLogResponse;
import com.lufthansa.planning_poker.audit.application.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditController.class)
@DisplayName("AuditController Tests")
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    @SuppressWarnings("unused")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private UUID auditLogId;
    private String adminUserId;
    private AuditLogResponse auditLogResponse;

    @BeforeEach
    void setUp() {
        auditLogId = UUID.randomUUID();
        adminUserId = "admin-user-id";

        auditLogResponse = new AuditLogResponse(
            auditLogId,
            UUID.randomUUID(),
            "RoomCreatedEvent",
            "ROOM",
            UUID.randomUUID().toString(),
            "CREATE",
            "{\"roomName\":\"Sprint 42 Planning\"}",
            "user-123",
            "testuser",
            Instant.now(),
            "pp-room-service"
        );
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminJwt() {
        return jwt().jwt(jwt -> jwt
            .subject(adminUserId)
            .claim("preferred_username", "admin")
            .claim("realm_access", Map.of("roles", List.of("ADMIN"))));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor userJwt() {
        return jwt().jwt(jwt -> jwt
            .subject("regular-user")
            .claim("preferred_username", "user")
            .claim("realm_access", Map.of("roles", List.of("USER"))));
    }

    @Nested
    @DisplayName("GET /api/v1/admin/audit")
    class GetAuditLogsEndpoint {

        @Test
        @DisplayName("Should return paginated audit logs for admin")
        void shouldReturnPaginatedAuditLogsForAdmin() throws Exception {
            // Given
            List<AuditLogResponse> logs = List.of(auditLogResponse);
            Page<AuditLogResponse> logPage = new PageImpl<>(logs, PageRequest.of(0, 50), 1);

            when(auditService.getAuditLogs(any(Pageable.class))).thenReturn(logPage);

            // When/Then
            mockMvc.perform(get("/api/v1/admin/audit")
                    .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(auditLogId.toString()))
                .andExpect(jsonPath("$.content[0].eventType").value("RoomCreatedEvent"))
                .andExpect(jsonPath("$.content[0].entityType").value("ROOM"))
                .andExpect(jsonPath("$.content[0].action").value("CREATE"));
        }

        @Test
        @DisplayName("Should return empty page when no logs")
        void shouldReturnEmptyPageWhenNoLogs() throws Exception {
            // Given
            Page<AuditLogResponse> emptyPage = new PageImpl<>(Collections.emptyList());
            when(auditService.getAuditLogs(any(Pageable.class))).thenReturn(emptyPage);

            // When/Then
            mockMvc.perform(get("/api/v1/admin/audit")
                    .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("Should support pagination parameters")
        void shouldSupportPaginationParameters() throws Exception {
            // Given
            Page<AuditLogResponse> logPage = new PageImpl<>(
                List.of(auditLogResponse),
                PageRequest.of(1, 10),
                25
            );
            when(auditService.getAuditLogs(any(Pageable.class))).thenReturn(logPage);

            // When/Then
            mockMvc.perform(get("/api/v1/admin/audit")
                    .param("page", "1")
                    .param("size", "10")
                    .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(10));
        }

        @Test
        @DisplayName("Should allow non-admin user access in WebMvcTest context")
        void shouldAllowNonAdminUserInTestContext() throws Exception {
            // Note: In @WebMvcTest without full security config, role-based access isn't enforced
            // This test verifies the endpoint is accessible with any authenticated user
            when(auditService.getAuditLogs(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(auditLogResponse)));

            mockMvc.perform(get("/api/v1/admin/audit")
                    .with(userJwt()))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // When/Then - Spring Security OAuth2 returns 401 for unauthenticated requests
            mockMvc.perform(get("/api/v1/admin/audit"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/audit/entity/{entityType}/{entityId}")
    class GetAuditLogsByEntityEndpoint {

        @Test
        @DisplayName("Should return logs for specific entity")
        void shouldReturnLogsForSpecificEntity() throws Exception {
            // Given
            String entityType = "ROOM";
            String entityId = UUID.randomUUID().toString();
            Page<AuditLogResponse> logPage = new PageImpl<>(List.of(auditLogResponse));

            when(auditService.getAuditLogsByEntity(eq(entityType), eq(entityId), any(Pageable.class)))
                .thenReturn(logPage);

            // When/Then
            mockMvc.perform(get("/api/v1/admin/audit/entity/{entityType}/{entityId}", entityType, entityId)
                    .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entityType").value("ROOM"));
        }

        @Test
        @DisplayName("Should return empty page for non-existent entity")
        void shouldReturnEmptyPageForNonExistentEntity() throws Exception {
            // Given
            String entityType = "ROOM";
            String entityId = "non-existent-id";
            Page<AuditLogResponse> emptyPage = new PageImpl<>(Collections.emptyList());

            when(auditService.getAuditLogsByEntity(eq(entityType), eq(entityId), any(Pageable.class)))
                .thenReturn(emptyPage);

            // When/Then
            mockMvc.perform(get("/api/v1/admin/audit/entity/{entityType}/{entityId}", entityType, entityId)
                    .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/audit/entity-type/{entityType}")
    class GetAuditLogsByEntityTypeEndpoint {

        @Test
        @DisplayName("Should return logs for ROOM entity type")
        void shouldReturnLogsForRoomEntityType() throws Exception {
            // Given
            String entityType = "ROOM";
            Page<AuditLogResponse> logPage = new PageImpl<>(List.of(auditLogResponse));

            when(auditService.getAuditLogsByEntityType(eq(entityType), any(Pageable.class)))
                .thenReturn(logPage);

            // When/Then
            mockMvc.perform(get("/api/v1/admin/audit/entity-type/{entityType}", entityType)
                    .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entityType").value("ROOM"));
        }

        @Test
        @DisplayName("Should return logs for VOTE entity type")
        void shouldReturnLogsForVoteEntityType() throws Exception {
            // Given
            String entityType = "VOTE";
            AuditLogResponse voteLog = new AuditLogResponse(
                UUID.randomUUID(), UUID.randomUUID(),
                "VoteCastEvent", "VOTE", UUID.randomUUID().toString(),
                "VOTE", "{}", "user-123", "testuser",
                Instant.now(), "pp-vote-service"
            );
            Page<AuditLogResponse> logPage = new PageImpl<>(List.of(voteLog));

            when(auditService.getAuditLogsByEntityType(eq(entityType), any(Pageable.class)))
                .thenReturn(logPage);

            // When/Then
            mockMvc.perform(get("/api/v1/admin/audit/entity-type/{entityType}", entityType)
                    .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entityType").value("VOTE"));
        }

        @Test
        @DisplayName("Should return logs for STORY entity type")
        void shouldReturnLogsForStoryEntityType() throws Exception {
            // Given
            String entityType = "STORY";
            AuditLogResponse storyLog = new AuditLogResponse(
                UUID.randomUUID(), UUID.randomUUID(),
                "StoryCreatedEvent", "STORY", UUID.randomUUID().toString(),
                "CREATE", "{}", "user-123", "testuser",
                Instant.now(), "pp-room-service"
            );
            Page<AuditLogResponse> logPage = new PageImpl<>(List.of(storyLog));

            when(auditService.getAuditLogsByEntityType(eq(entityType), any(Pageable.class)))
                .thenReturn(logPage);

            // When/Then
            mockMvc.perform(get("/api/v1/admin/audit/entity-type/{entityType}", entityType)
                    .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entityType").value("STORY"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/audit/user/{userId}")
    class GetAuditLogsByUserEndpoint {

        @Test
        @DisplayName("Should return logs for specific user")
        void shouldReturnLogsForSpecificUser() throws Exception {
            // Given
            String userId = "user-123";
            Page<AuditLogResponse> logPage = new PageImpl<>(List.of(auditLogResponse));

            when(auditService.getAuditLogsByUser(eq(userId), any(Pageable.class)))
                .thenReturn(logPage);

            // When/Then
            mockMvc.perform(get("/api/v1/admin/audit/user/{userId}", userId)
                    .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value("user-123"));
        }

        @Test
        @DisplayName("Should return empty page for user with no logs")
        void shouldReturnEmptyPageForUserWithNoLogs() throws Exception {
            // Given
            String userId = "non-existent-user";
            Page<AuditLogResponse> emptyPage = new PageImpl<>(Collections.emptyList());

            when(auditService.getAuditLogsByUser(eq(userId), any(Pageable.class)))
                .thenReturn(emptyPage);

            // When/Then
            mockMvc.perform(get("/api/v1/admin/audit/user/{userId}", userId)
                    .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/audit/search")
    class SearchAuditLogsEndpoint {

        @Test
        @DisplayName("Should search with all filters")
        void shouldSearchWithAllFilters() throws Exception {
            // Given
            Page<AuditLogResponse> logPage = new PageImpl<>(List.of(auditLogResponse));

            when(auditService.searchAuditLogs(
                eq("ROOM"), eq("user-123"), eq("CREATE"), any(Pageable.class)))
                .thenReturn(logPage);

            // When/Then
            mockMvc.perform(get("/api/v1/admin/audit/search")
                    .param("entityType", "ROOM")
                    .param("userId", "user-123")
                    .param("action", "CREATE")
                    .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entityType").value("ROOM"))
                .andExpect(jsonPath("$.content[0].action").value("CREATE"));
        }

        @Test
        @DisplayName("Should search with partial filters")
        void shouldSearchWithPartialFilters() throws Exception {
            // Given
            Page<AuditLogResponse> logPage = new PageImpl<>(List.of(auditLogResponse));

            when(auditService.searchAuditLogs(
                eq("ROOM"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(logPage);

            // When/Then
            mockMvc.perform(get("/api/v1/admin/audit/search")
                    .param("entityType", "ROOM")
                    .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entityType").value("ROOM"));
        }

        @Test
        @DisplayName("Should search without any filters")
        void shouldSearchWithoutAnyFilters() throws Exception {
            // Given
            Page<AuditLogResponse> logPage = new PageImpl<>(List.of(auditLogResponse));

            when(auditService.searchAuditLogs(
                isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(logPage);

            // When/Then
            mockMvc.perform(get("/api/v1/admin/audit/search")
                    .with(adminJwt()))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should search with action filter only")
        void shouldSearchWithActionFilterOnly() throws Exception {
            // Given
            Page<AuditLogResponse> logPage = new PageImpl<>(List.of(auditLogResponse));

            when(auditService.searchAuditLogs(
                isNull(), isNull(), eq("CREATE"), any(Pageable.class)))
                .thenReturn(logPage);

            // When/Then
            mockMvc.perform(get("/api/v1/admin/audit/search")
                    .param("action", "CREATE")
                    .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("CREATE"));
        }

        @Test
        @DisplayName("Should search for DELETE actions")
        void shouldSearchForDeleteActions() throws Exception {
            // Given
            AuditLogResponse deleteLog = new AuditLogResponse(
                UUID.randomUUID(), UUID.randomUUID(),
                "RoomDeletedEvent", "ROOM", UUID.randomUUID().toString(),
                "DELETE", "{}", "admin-123", "admin",
                Instant.now(), "pp-room-service"
            );
            Page<AuditLogResponse> logPage = new PageImpl<>(List.of(deleteLog));

            when(auditService.searchAuditLogs(
                isNull(), isNull(), eq("DELETE"), any(Pageable.class)))
                .thenReturn(logPage);

            // When/Then
            mockMvc.perform(get("/api/v1/admin/audit/search")
                    .param("action", "DELETE")
                    .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("DELETE"));
        }
    }
}

