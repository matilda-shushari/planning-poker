package com.lufthansa.planning_poker.room.infrastructure.messaging;

import com.lufthansa.planning_poker.common.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomEventProducer Tests")
class RoomEventProducerTest {

    @Mock
    private KafkaTemplate<String, BaseEvent> kafkaTemplate;

    private RoomEventProducer roomEventProducer;

    @BeforeEach
    void setUp() {
        roomEventProducer = new RoomEventProducer(kafkaTemplate);
    }

    @Test
    @DisplayName("Should publish RoomCreatedEvent to Kafka")
    void shouldPublishRoomCreatedEvent() {
        // Given
        UUID roomId = UUID.randomUUID();
        RoomCreatedEvent event = RoomCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .roomId(roomId)
                .roomName("Sprint 42 Planning")
                .moderatorId("user-123")
                .moderatorName("John Doe")
                .deckType("FIBONACCI")
                .triggeredBy("user-123")
                .triggeredByName("John Doe")
                .timestamp(Instant.now())
                .build();

        // When
        roomEventProducer.publishRoomCreated(event);

        // Then
        ArgumentCaptor<BaseEvent> eventCaptor = ArgumentCaptor.forClass(BaseEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.ROOM_EVENTS), eq(roomId.toString()), eventCaptor.capture());
        
        assertThat(eventCaptor.getValue()).isInstanceOf(RoomCreatedEvent.class);
        RoomCreatedEvent capturedEvent = (RoomCreatedEvent) eventCaptor.getValue();
        assertThat(capturedEvent.getRoomId()).isEqualTo(roomId);
        assertThat(capturedEvent.getRoomName()).isEqualTo("Sprint 42 Planning");
    }

    @Test
    @DisplayName("Should publish RoomUpdatedEvent to Kafka")
    void shouldPublishRoomUpdatedEvent() {
        // Given
        UUID roomId = UUID.randomUUID();
        RoomUpdatedEvent event = RoomUpdatedEvent.builder()
                .eventId(UUID.randomUUID())
                .roomId(roomId)
                .roomName("Updated Sprint Planning")
                .description("Updated description")
                .triggeredBy("user-456")
                .triggeredByName("Jane Doe")
                .timestamp(Instant.now())
                .build();

        // When
        roomEventProducer.publishRoomUpdated(event);

        // Then
        ArgumentCaptor<BaseEvent> eventCaptor = ArgumentCaptor.forClass(BaseEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.ROOM_EVENTS), eq(roomId.toString()), eventCaptor.capture());
        
        assertThat(eventCaptor.getValue()).isInstanceOf(RoomUpdatedEvent.class);
        RoomUpdatedEvent capturedEvent = (RoomUpdatedEvent) eventCaptor.getValue();
        assertThat(capturedEvent.getRoomId()).isEqualTo(roomId);
    }

    @Test
    @DisplayName("Should publish RoomDeletedEvent to Kafka")
    void shouldPublishRoomDeletedEvent() {
        // Given
        UUID roomId = UUID.randomUUID();
        RoomDeletedEvent event = RoomDeletedEvent.builder()
                .eventId(UUID.randomUUID())
                .roomId(roomId)
                .roomName("Deleted Room")
                .reason("Session ended")
                .triggeredBy("admin-user")
                .triggeredByName("Admin")
                .timestamp(Instant.now())
                .build();

        // When
        roomEventProducer.publishRoomDeleted(event);

        // Then
        ArgumentCaptor<BaseEvent> eventCaptor = ArgumentCaptor.forClass(BaseEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.ROOM_EVENTS), eq(roomId.toString()), eventCaptor.capture());
        
        assertThat(eventCaptor.getValue()).isInstanceOf(RoomDeletedEvent.class);
    }

    @Test
    @DisplayName("Should publish StoryCreatedEvent to Kafka")
    void shouldPublishStoryCreatedEvent() {
        // Given
        UUID storyId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        StoryCreatedEvent event = StoryCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .storyId(storyId)
                .roomId(roomId)
                .title("Implement User Login")
                .triggeredBy("user-123")
                .triggeredByName("John")
                .timestamp(Instant.now())
                .build();

        // When
        roomEventProducer.publishStoryCreated(event);

        // Then
        ArgumentCaptor<BaseEvent> eventCaptor = ArgumentCaptor.forClass(BaseEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.STORY_EVENTS), eq(roomId.toString()), eventCaptor.capture());
        
        assertThat(eventCaptor.getValue()).isInstanceOf(StoryCreatedEvent.class);
        StoryCreatedEvent capturedEvent = (StoryCreatedEvent) eventCaptor.getValue();
        assertThat(capturedEvent.getStoryId()).isEqualTo(storyId);
        assertThat(capturedEvent.getTitle()).isEqualTo("Implement User Login");
    }

    @Test
    @DisplayName("Should publish StoryUpdatedEvent to Kafka")
    void shouldPublishStoryUpdatedEvent() {
        // Given
        UUID storyId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        StoryUpdatedEvent event = StoryUpdatedEvent.builder()
                .eventId(UUID.randomUUID())
                .storyId(storyId)
                .roomId(roomId)
                .title("Updated Story Title")
                .triggeredBy("user-456")
                .triggeredByName("Jane")
                .timestamp(Instant.now())
                .build();

        // When
        roomEventProducer.publishStoryUpdated(event);

        // Then
        ArgumentCaptor<BaseEvent> eventCaptor = ArgumentCaptor.forClass(BaseEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.STORY_EVENTS), eq(roomId.toString()), eventCaptor.capture());
        
        assertThat(eventCaptor.getValue()).isInstanceOf(StoryUpdatedEvent.class);
    }

    @Test
    @DisplayName("Should publish StoryDeletedEvent to Kafka")
    void shouldPublishStoryDeletedEvent() {
        // Given
        UUID storyId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        StoryDeletedEvent event = StoryDeletedEvent.builder()
                .eventId(UUID.randomUUID())
                .storyId(storyId)
                .roomId(roomId)
                .title("Deleted Story")
                .triggeredBy("moderator-1")
                .triggeredByName("Moderator")
                .timestamp(Instant.now())
                .build();

        // When
        roomEventProducer.publishStoryDeleted(event);

        // Then
        ArgumentCaptor<BaseEvent> eventCaptor = ArgumentCaptor.forClass(BaseEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.STORY_EVENTS), eq(roomId.toString()), eventCaptor.capture());
        
        assertThat(eventCaptor.getValue()).isInstanceOf(StoryDeletedEvent.class);
    }
}
