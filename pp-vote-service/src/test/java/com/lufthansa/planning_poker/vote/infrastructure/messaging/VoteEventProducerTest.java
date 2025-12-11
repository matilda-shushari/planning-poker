package com.lufthansa.planning_poker.vote.infrastructure.messaging;

import com.lufthansa.planning_poker.common.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoteEventProducer Tests")
class VoteEventProducerTest {

    @Mock
    private KafkaTemplate<String, BaseEvent> kafkaTemplate;

    private VoteEventProducer voteEventProducer;

    @BeforeEach
    void setUp() {
        voteEventProducer = new VoteEventProducer(kafkaTemplate);
    }

    @Test
    @DisplayName("Should publish VoteCastEvent to Kafka")
    void shouldPublishVoteCastEvent() {
        // Given
        UUID storyId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        VoteCastEvent event = VoteCastEvent.builder()
                .eventId(UUID.randomUUID())
                .storyId(storyId)
                .roomId(roomId)
                .userId("user-123")
                .userName("Alice")
                .value("8")
                .isUpdate(false)
                .triggeredBy("user-123")
                .triggeredByName("Alice")
                .timestamp(Instant.now())
                .build();

        // When
        voteEventProducer.publishVoteCast(event);

        // Then
        ArgumentCaptor<BaseEvent> eventCaptor = ArgumentCaptor.forClass(BaseEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.VOTE_EVENTS), eq(storyId.toString()), eventCaptor.capture());
        
        assertThat(eventCaptor.getValue()).isInstanceOf(VoteCastEvent.class);
        VoteCastEvent capturedEvent = (VoteCastEvent) eventCaptor.getValue();
        assertThat(capturedEvent.getStoryId()).isEqualTo(storyId);
        assertThat(capturedEvent.getValue()).isEqualTo("8");
    }

    @Test
    @DisplayName("Should publish VotingStartedEvent to Kafka")
    void shouldPublishVotingStartedEvent() {
        // Given
        UUID storyId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        VotingStartedEvent event = VotingStartedEvent.builder()
                .eventId(UUID.randomUUID())
                .storyId(storyId)
                .roomId(roomId)
                .storyTitle("User Authentication")
                .triggeredBy("moderator-1")
                .triggeredByName("Moderator")
                .timestamp(Instant.now())
                .build();

        // When
        voteEventProducer.publishVotingStarted(event);

        // Then
        ArgumentCaptor<BaseEvent> eventCaptor = ArgumentCaptor.forClass(BaseEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.VOTE_EVENTS), eq(roomId.toString()), eventCaptor.capture());
        
        assertThat(eventCaptor.getValue()).isInstanceOf(VotingStartedEvent.class);
        VotingStartedEvent capturedEvent = (VotingStartedEvent) eventCaptor.getValue();
        assertThat(capturedEvent.getStoryId()).isEqualTo(storyId);
        assertThat(capturedEvent.getStoryTitle()).isEqualTo("User Authentication");
    }

    @Test
    @DisplayName("Should publish VotingFinishedEvent to Kafka")
    void shouldPublishVotingFinishedEvent() {
        // Given
        UUID storyId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        VotingFinishedEvent event = VotingFinishedEvent.builder()
                .eventId(UUID.randomUUID())
                .storyId(storyId)
                .roomId(roomId)
                .storyTitle("Payment Gateway Integration")
                .averageScore(new BigDecimal("5.5"))
                .finalEstimate("5")
                .totalVotes(5)
                .triggeredBy("moderator-1")
                .triggeredByName("Moderator")
                .timestamp(Instant.now())
                .build();

        // When
        voteEventProducer.publishVotingFinished(event);

        // Then
        ArgumentCaptor<BaseEvent> eventCaptor = ArgumentCaptor.forClass(BaseEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.VOTE_EVENTS), eq(roomId.toString()), eventCaptor.capture());
        
        assertThat(eventCaptor.getValue()).isInstanceOf(VotingFinishedEvent.class);
        VotingFinishedEvent capturedEvent = (VotingFinishedEvent) eventCaptor.getValue();
        assertThat(capturedEvent.getStoryId()).isEqualTo(storyId);
        assertThat(capturedEvent.getAverageScore()).isEqualTo(new BigDecimal("5.5"));
        assertThat(capturedEvent.getFinalEstimate()).isEqualTo("5");
        assertThat(capturedEvent.getTotalVotes()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should use story ID as partition key for VoteCastEvent")
    void shouldUseStoryIdAsPartitionKeyForVoteCast() {
        // Given
        UUID storyId = UUID.randomUUID();
        VoteCastEvent event = VoteCastEvent.builder()
                .eventId(UUID.randomUUID())
                .storyId(storyId)
                .roomId(UUID.randomUUID())
                .userId("user-1")
                .userName("User One")
                .value("3")
                .isUpdate(false)
                .triggeredBy("user-1")
                .triggeredByName("User One")
                .timestamp(Instant.now())
                .build();

        // When
        voteEventProducer.publishVoteCast(event);

        // Then
        verify(kafkaTemplate).send(eq(KafkaTopics.VOTE_EVENTS), eq(storyId.toString()), eq(event));
    }

    @Test
    @DisplayName("Should use room ID as partition key for VotingStartedEvent")
    void shouldUseRoomIdAsPartitionKeyForVotingStarted() {
        // Given
        UUID roomId = UUID.randomUUID();
        VotingStartedEvent event = VotingStartedEvent.builder()
                .eventId(UUID.randomUUID())
                .storyId(UUID.randomUUID())
                .roomId(roomId)
                .storyTitle("Test Story")
                .triggeredBy("mod-1")
                .triggeredByName("Moderator")
                .timestamp(Instant.now())
                .build();

        // When
        voteEventProducer.publishVotingStarted(event);

        // Then
        verify(kafkaTemplate).send(eq(KafkaTopics.VOTE_EVENTS), eq(roomId.toString()), eq(event));
    }
}
