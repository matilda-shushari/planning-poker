package com.lufthansa.planning_poker.vote.infrastructure.messaging;

import com.lufthansa.planning_poker.common.event.BaseEvent;
import com.lufthansa.planning_poker.common.event.KafkaTopics;
import com.lufthansa.planning_poker.common.event.VotingStartedEvent;
import com.lufthansa.planning_poker.vote.api.websocket.VotingWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes story events from Kafka to trigger WebSocket notifications.
 * <p>
 * When voting starts on a story in Room Service, this consumer
 * broadcasts the event to all connected WebSocket clients.
 * </p>
 *
 * @author Matilda Dervishaj
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StoryEventConsumer {

    private final VotingWebSocketHandler webSocketHandler;

    @KafkaListener(
        topics = KafkaTopics.STORY_EVENTS,
        groupId = KafkaTopics.VOTE_CONSUMER_GROUP,
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeStoryEvents(BaseEvent event) {
        if (event instanceof VotingStartedEvent votingStarted) {
            handleVotingStarted(votingStarted);
        }
    }

    private void handleVotingStarted(VotingStartedEvent event) {
        log.info("Received VotingStartedEvent for story: {} in room: {}", 
            event.getStoryId(), event.getRoomId());
        
        // Broadcast to all participants via WebSocket
        webSocketHandler.broadcastVotingStarted(
            event.getRoomId(),
            event.getStoryId(),
            event.getStoryTitle()
        );
    }
}

