package com.lufthansa.planning_poker.audit.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lufthansa.planning_poker.audit.infrastructure.persistence.entity.AuditLogEntity;
import com.lufthansa.planning_poker.audit.infrastructure.persistence.entity.AuditLogEntity.AuditAction;
import com.lufthansa.planning_poker.audit.infrastructure.persistence.repository.JpaAuditLogRepository;
import com.lufthansa.planning_poker.common.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditEventConsumer Tests")
class AuditEventConsumerTest {

    @Mock
    private JpaAuditLogRepository auditLogRepository;

    private ObjectMapper objectMapper;
    private AuditEventConsumer auditEventConsumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        auditEventConsumer = new AuditEventConsumer(auditLogRepository, objectMapper);
    }

    @Nested
    @DisplayName("consumeRoomEvents")
    class ConsumeRoomEvents {

        @Test
        @DisplayName("Should save audit log for RoomCreatedEvent")
        void shouldSaveAuditLogForRoomCreated() {
            // Given
            UUID roomId = UUID.randomUUID();
            RoomCreatedEvent event = RoomCreatedEvent.builder()
                    .eventId(UUID.randomUUID())
                    .eventType("ROOM_CREATED")
                    .roomId(roomId)
                    .roomName("Sprint Planning")
                    .moderatorId("mod-123")
                    .moderatorName("John Moderator")
                    .deckType("FIBONACCI")
                    .triggeredBy("mod-123")
                    .triggeredByName("John Moderator")
                    .timestamp(Instant.now())
                    .build();

            when(auditLogRepository.save(any(AuditLogEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            auditEventConsumer.consumeRoomEvents(event);

            // Then
            ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
            verify(auditLogRepository).save(captor.capture());

            AuditLogEntity savedLog = captor.getValue();
            assertThat(savedLog.getEntityType()).isEqualTo("ROOM");
            assertThat(savedLog.getEntityId()).isEqualTo(roomId.toString());
            assertThat(savedLog.getAction()).isEqualTo(AuditAction.CREATE);
            assertThat(savedLog.getSourceService()).isEqualTo("pp-room-service");
        }

        @Test
        @DisplayName("Should save audit log for RoomUpdatedEvent")
        void shouldSaveAuditLogForRoomUpdated() {
            // Given
            UUID roomId = UUID.randomUUID();
            RoomUpdatedEvent event = RoomUpdatedEvent.builder()
                    .eventId(UUID.randomUUID())
                    .eventType("ROOM_UPDATED")
                    .roomId(roomId)
                    .roomName("Updated Sprint Planning")
                    .description("Updated description")
                    .triggeredBy("mod-123")
                    .triggeredByName("John")
                    .timestamp(Instant.now())
                    .build();

            when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            auditEventConsumer.consumeRoomEvents(event);

            // Then
            ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
            verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.UPDATE);
        }

        @Test
        @DisplayName("Should save audit log for RoomDeletedEvent")
        void shouldSaveAuditLogForRoomDeleted() {
            // Given
            UUID roomId = UUID.randomUUID();
            RoomDeletedEvent event = RoomDeletedEvent.builder()
                    .eventId(UUID.randomUUID())
                    .eventType("ROOM_DELETED")
                    .roomId(roomId)
                    .roomName("Deleted Room")
                    .reason("Session ended")
                    .triggeredBy("admin-1")
                    .triggeredByName("Admin")
                    .timestamp(Instant.now())
                    .build();

            when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            auditEventConsumer.consumeRoomEvents(event);

            // Then
            ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
            verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.DELETE);
        }
    }

    @Nested
    @DisplayName("consumeStoryEvents")
    class ConsumeStoryEvents {

        @Test
        @DisplayName("Should save audit log for StoryCreatedEvent")
        void shouldSaveAuditLogForStoryCreated() {
            // Given
            UUID storyId = UUID.randomUUID();
            UUID roomId = UUID.randomUUID();
            StoryCreatedEvent event = StoryCreatedEvent.builder()
                    .eventId(UUID.randomUUID())
                    .eventType("STORY_CREATED")
                    .storyId(storyId)
                    .roomId(roomId)
                    .title("User Login Feature")
                    .description("Implement login")
                    .triggeredBy("mod-1")
                    .triggeredByName("Moderator")
                    .timestamp(Instant.now())
                    .build();

            when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            auditEventConsumer.consumeStoryEvents(event);

            // Then
            ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
            verify(auditLogRepository).save(captor.capture());

            AuditLogEntity savedLog = captor.getValue();
            assertThat(savedLog.getEntityType()).isEqualTo("STORY");
            assertThat(savedLog.getEntityId()).isEqualTo(storyId.toString());
            assertThat(savedLog.getAction()).isEqualTo(AuditAction.CREATE);
            assertThat(savedLog.getSourceService()).isEqualTo("pp-room-service");
        }

        @Test
        @DisplayName("Should save audit log for StoryUpdatedEvent")
        void shouldSaveAuditLogForStoryUpdated() {
            // Given
            StoryUpdatedEvent event = StoryUpdatedEvent.builder()
                    .eventId(UUID.randomUUID())
                    .eventType("STORY_UPDATED")
                    .storyId(UUID.randomUUID())
                    .roomId(UUID.randomUUID())
                    .title("Updated Story")
                    .triggeredBy("mod-1")
                    .triggeredByName("Mod")
                    .timestamp(Instant.now())
                    .build();

            when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            auditEventConsumer.consumeStoryEvents(event);

            // Then
            ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
            verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.UPDATE);
        }

        @Test
        @DisplayName("Should save audit log for StoryDeletedEvent")
        void shouldSaveAuditLogForStoryDeleted() {
            // Given
            StoryDeletedEvent event = StoryDeletedEvent.builder()
                    .eventId(UUID.randomUUID())
                    .eventType("STORY_DELETED")
                    .storyId(UUID.randomUUID())
                    .roomId(UUID.randomUUID())
                    .title("Deleted Story")
                    .triggeredBy("mod-1")
                    .triggeredByName("Mod")
                    .timestamp(Instant.now())
                    .build();

            when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            auditEventConsumer.consumeStoryEvents(event);

            // Then
            ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
            verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.DELETE);
        }
    }

    @Nested
    @DisplayName("consumeVoteEvents")
    class ConsumeVoteEvents {

        @Test
        @DisplayName("Should save audit log for VoteCastEvent")
        void shouldSaveAuditLogForVoteCast() {
            // Given
            UUID storyId = UUID.randomUUID();
            VoteCastEvent event = VoteCastEvent.builder()
                    .eventId(UUID.randomUUID())
                    .eventType("VOTE_CAST")
                    .storyId(storyId)
                    .roomId(UUID.randomUUID())
                    .userId("user-1")
                    .userName("Alice")
                    .value("8")
                    .isUpdate(false)
                    .triggeredBy("user-1")
                    .triggeredByName("Alice")
                    .timestamp(Instant.now())
                    .build();

            when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            auditEventConsumer.consumeVoteEvents(event);

            // Then
            ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
            verify(auditLogRepository).save(captor.capture());

            AuditLogEntity savedLog = captor.getValue();
            assertThat(savedLog.getEntityType()).isEqualTo("VOTE");
            assertThat(savedLog.getEntityId()).isEqualTo(storyId.toString());
            assertThat(savedLog.getAction()).isEqualTo(AuditAction.VOTE);
            assertThat(savedLog.getSourceService()).isEqualTo("pp-vote-service");
        }

        @Test
        @DisplayName("Should save audit log for VotingStartedEvent")
        void shouldSaveAuditLogForVotingStarted() {
            // Given
            VotingStartedEvent event = VotingStartedEvent.builder()
                    .eventId(UUID.randomUUID())
                    .eventType("VOTING_STARTED")
                    .storyId(UUID.randomUUID())
                    .roomId(UUID.randomUUID())
                    .storyTitle("Story Title")
                    .triggeredBy("mod-1")
                    .triggeredByName("Moderator")
                    .timestamp(Instant.now())
                    .build();

            when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            auditEventConsumer.consumeVoteEvents(event);

            // Then
            ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
            verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.START);
        }

        @Test
        @DisplayName("Should save audit log for VotingFinishedEvent")
        void shouldSaveAuditLogForVotingFinished() {
            // Given
            VotingFinishedEvent event = VotingFinishedEvent.builder()
                    .eventId(UUID.randomUUID())
                    .eventType("VOTING_FINISHED")
                    .storyId(UUID.randomUUID())
                    .roomId(UUID.randomUUID())
                    .storyTitle("Story Title")
                    .averageScore(new BigDecimal("5.5"))
                    .finalEstimate("5")
                    .totalVotes(5)
                    .triggeredBy("mod-1")
                    .triggeredByName("Moderator")
                    .timestamp(Instant.now())
                    .build();

            when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            auditEventConsumer.consumeVoteEvents(event);

            // Then
            ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
            verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.FINISH);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle repository save failure gracefully")
        void shouldHandleRepositoryFailure() {
            // Given
            RoomCreatedEvent event = createRoomCreatedEvent();
            when(auditLogRepository.save(any())).thenThrow(new RuntimeException("Database error"));

            // When - Should not throw exception
            auditEventConsumer.consumeRoomEvents(event);

            // Then
            verify(auditLogRepository).save(any());
        }
    }

    @Nested
    @DisplayName("Source Service Determination")
    class SourceServiceDetermination {

        @Test
        @DisplayName("Should set source service to pp-room-service for room events")
        void shouldSetRoomServiceAsSource() {
            // Given
            RoomCreatedEvent event = createRoomCreatedEvent();
            when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            auditEventConsumer.consumeRoomEvents(event);

            // Then
            ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
            verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getSourceService()).isEqualTo("pp-room-service");
        }

        @Test
        @DisplayName("Should set source service to pp-vote-service for vote events")
        void shouldSetVoteServiceAsSource() {
            // Given
            VoteCastEvent event = VoteCastEvent.builder()
                    .eventId(UUID.randomUUID())
                    .eventType("VOTE_CAST")
                    .storyId(UUID.randomUUID())
                    .roomId(UUID.randomUUID())
                    .userId("user-1")
                    .userName("User")
                    .value("5")
                    .isUpdate(false)
                    .triggeredBy("user-1")
                    .triggeredByName("User")
                    .timestamp(Instant.now())
                    .build();
            when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            auditEventConsumer.consumeVoteEvents(event);

            // Then
            ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
            verify(auditLogRepository).save(captor.capture());
            assertThat(captor.getValue().getSourceService()).isEqualTo("pp-vote-service");
        }
    }

    private RoomCreatedEvent createRoomCreatedEvent() {
        return RoomCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("ROOM_CREATED")
                .roomId(UUID.randomUUID())
                .roomName("Test Room")
                .moderatorId("mod-1")
                .moderatorName("Moderator")
                .deckType("FIBONACCI")
                .triggeredBy("mod-1")
                .triggeredByName("Moderator")
                .timestamp(Instant.now())
                .build();
    }
}
