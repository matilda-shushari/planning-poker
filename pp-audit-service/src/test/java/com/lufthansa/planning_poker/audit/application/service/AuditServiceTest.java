package com.lufthansa.planning_poker.audit.application.service;

import com.lufthansa.planning_poker.audit.application.dto.response.AuditLogResponse;
import com.lufthansa.planning_poker.audit.infrastructure.persistence.entity.AuditLogEntity;
import com.lufthansa.planning_poker.audit.infrastructure.persistence.repository.JpaAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService Tests")
class AuditServiceTest {

    @Mock
    private JpaAuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private UUID auditLogId;
    private AuditLogEntity auditLogEntity;

    @BeforeEach
    void setUp() {
        auditLogId = UUID.randomUUID();

        auditLogEntity = AuditLogEntity.builder()
            .id(auditLogId)
            .eventId(UUID.randomUUID())
            .eventType("RoomCreatedEvent")
            .entityType("ROOM")
            .entityId(UUID.randomUUID().toString())
            .action(AuditLogEntity.AuditAction.CREATE)
            .eventData("{\"roomName\":\"Sprint 42\"}")
            .userId("user-123")
            .userName("testuser")
            .timestamp(Instant.now())
            .sourceService("pp-room-service")
            .build();
    }

    @Nested
    @DisplayName("getAuditLogs Tests")
    class GetAuditLogsTests {

        @Test
        @DisplayName("Should return paginated audit logs")
        void shouldReturnPaginatedAuditLogs() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<AuditLogEntity> logs = List.of(auditLogEntity);
            Page<AuditLogEntity> logPage = new PageImpl<>(logs, pageable, 1);

            when(auditLogRepository.findAll(pageable)).thenReturn(logPage);

            // When
            Page<AuditLogResponse> result = auditService.getAuditLogs(pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).eventType()).isEqualTo("RoomCreatedEvent");
        }

        @Test
        @DisplayName("Should return empty page when no logs")
        void shouldReturnEmptyPageWhenNoLogs() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<AuditLogEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(auditLogRepository.findAll(pageable)).thenReturn(emptyPage);

            // When
            Page<AuditLogResponse> result = auditService.getAuditLogs(pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should correctly map all fields to response")
        void shouldCorrectlyMapAllFieldsToResponse() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<AuditLogEntity> logs = List.of(auditLogEntity);
            Page<AuditLogEntity> logPage = new PageImpl<>(logs, pageable, 1);

            when(auditLogRepository.findAll(pageable)).thenReturn(logPage);

            // When
            Page<AuditLogResponse> result = auditService.getAuditLogs(pageable);

            // Then
            AuditLogResponse response = result.getContent().get(0);
            assertThat(response.id()).isEqualTo(auditLogId);
            assertThat(response.eventType()).isEqualTo("RoomCreatedEvent");
            assertThat(response.entityType()).isEqualTo("ROOM");
            assertThat(response.action()).isEqualTo("CREATE");
            assertThat(response.userId()).isEqualTo("user-123");
            assertThat(response.userName()).isEqualTo("testuser");
            assertThat(response.sourceService()).isEqualTo("pp-room-service");
        }
    }

    @Nested
    @DisplayName("getAuditLogsByEntity Tests")
    class GetAuditLogsByEntityTests {

        @Test
        @DisplayName("Should return logs for specific entity")
        void shouldReturnLogsForSpecificEntity() {
            // Given
            String entityType = "ROOM";
            String entityId = auditLogEntity.getEntityId();
            Pageable pageable = PageRequest.of(0, 10);
            List<AuditLogEntity> logs = List.of(auditLogEntity);
            Page<AuditLogEntity> logPage = new PageImpl<>(logs, pageable, 1);

            when(auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable))
                .thenReturn(logPage);

            // When
            Page<AuditLogResponse> result = auditService.getAuditLogsByEntity(entityType, entityId, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).entityType()).isEqualTo("ROOM");
        }
    }

    @Nested
    @DisplayName("getAuditLogsByEntityType Tests")
    class GetAuditLogsByEntityTypeTests {

        @Test
        @DisplayName("Should return logs for entity type ROOM")
        void shouldReturnLogsForEntityTypeRoom() {
            // Given
            String entityType = "ROOM";
            Pageable pageable = PageRequest.of(0, 10);
            List<AuditLogEntity> logs = List.of(auditLogEntity);
            Page<AuditLogEntity> logPage = new PageImpl<>(logs, pageable, 1);

            when(auditLogRepository.findByEntityType(entityType, pageable)).thenReturn(logPage);

            // When
            Page<AuditLogResponse> result = auditService.getAuditLogsByEntityType(entityType, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).entityType()).isEqualTo("ROOM");
        }

        @Test
        @DisplayName("Should return logs for entity type VOTE")
        void shouldReturnLogsForEntityTypeVote() {
            // Given
            String entityType = "VOTE";
            Pageable pageable = PageRequest.of(0, 10);
            AuditLogEntity voteLog = AuditLogEntity.builder()
                .id(UUID.randomUUID())
                .eventType("VoteCastEvent")
                .entityType("VOTE")
                .action(AuditLogEntity.AuditAction.VOTE)
                .build();
            Page<AuditLogEntity> logPage = new PageImpl<>(List.of(voteLog), pageable, 1);

            when(auditLogRepository.findByEntityType(entityType, pageable)).thenReturn(logPage);

            // When
            Page<AuditLogResponse> result = auditService.getAuditLogsByEntityType(entityType, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).entityType()).isEqualTo("VOTE");
        }
    }

    @Nested
    @DisplayName("getAuditLogsByUser Tests")
    class GetAuditLogsByUserTests {

        @Test
        @DisplayName("Should return logs for specific user")
        void shouldReturnLogsForSpecificUser() {
            // Given
            String userId = "user-123";
            Pageable pageable = PageRequest.of(0, 10);
            List<AuditLogEntity> logs = List.of(auditLogEntity);
            Page<AuditLogEntity> logPage = new PageImpl<>(logs, pageable, 1);

            when(auditLogRepository.findByUserId(userId, pageable)).thenReturn(logPage);

            // When
            Page<AuditLogResponse> result = auditService.getAuditLogsByUser(userId, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).userId()).isEqualTo("user-123");
        }

        @Test
        @DisplayName("Should return empty page for user with no logs")
        void shouldReturnEmptyPageForUserWithNoLogs() {
            // Given
            String userId = "non-existent-user";
            Pageable pageable = PageRequest.of(0, 10);
            Page<AuditLogEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(auditLogRepository.findByUserId(userId, pageable)).thenReturn(emptyPage);

            // When
            Page<AuditLogResponse> result = auditService.getAuditLogsByUser(userId, pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchAuditLogs Tests")
    class SearchAuditLogsTests {

        @Test
        @DisplayName("Should search with all filters")
        void shouldSearchWithAllFilters() {
            // Given
            String entityType = "ROOM";
            String userId = "user-123";
            String action = "CREATE";
            Pageable pageable = PageRequest.of(0, 10);
            List<AuditLogEntity> logs = List.of(auditLogEntity);
            Page<AuditLogEntity> logPage = new PageImpl<>(logs, pageable, 1);

            when(auditLogRepository.findWithFilters(
                eq(entityType), eq(userId), eq(AuditLogEntity.AuditAction.CREATE), eq(pageable))
            ).thenReturn(logPage);

            // When
            Page<AuditLogResponse> result = auditService.searchAuditLogs(entityType, userId, action, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should search with null action filter")
        void shouldSearchWithNullActionFilter() {
            // Given
            String entityType = "ROOM";
            String userId = "user-123";
            Pageable pageable = PageRequest.of(0, 10);
            List<AuditLogEntity> logs = List.of(auditLogEntity);
            Page<AuditLogEntity> logPage = new PageImpl<>(logs, pageable, 1);

            when(auditLogRepository.findWithFilters(
                eq(entityType), eq(userId), isNull(), eq(pageable))
            ).thenReturn(logPage);

            // When
            Page<AuditLogResponse> result = auditService.searchAuditLogs(entityType, userId, null, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should search with VOTE action")
        void shouldSearchWithVoteAction() {
            // Given
            AuditLogEntity voteLog = AuditLogEntity.builder()
                .id(UUID.randomUUID())
                .eventType("VoteCastEvent")
                .entityType("VOTE")
                .action(AuditLogEntity.AuditAction.VOTE)
                .userId("user-123")
                .build();
            Pageable pageable = PageRequest.of(0, 10);
            Page<AuditLogEntity> logPage = new PageImpl<>(List.of(voteLog), pageable, 1);

            when(auditLogRepository.findWithFilters(
                eq("VOTE"), isNull(), eq(AuditLogEntity.AuditAction.VOTE), eq(pageable))
            ).thenReturn(logPage);

            // When
            Page<AuditLogResponse> result = auditService.searchAuditLogs("VOTE", null, "VOTE", pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).action()).isEqualTo("VOTE");
        }
    }
}

