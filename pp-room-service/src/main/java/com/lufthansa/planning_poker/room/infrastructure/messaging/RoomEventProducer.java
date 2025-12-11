package com.lufthansa.planning_poker.room.infrastructure.messaging;

import com.lufthansa.planning_poker.common.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomEventProducer {

    private final KafkaTemplate<String, BaseEvent> kafkaTemplate;

    public void publishRoomCreated(RoomCreatedEvent event) {
        log.info("Publishing RoomCreatedEvent for room: {}", event.getRoomId());
        kafkaTemplate.send(KafkaTopics.ROOM_EVENTS, event.getRoomId().toString(), event);
    }

    public void publishRoomUpdated(RoomUpdatedEvent event) {
        log.info("Publishing RoomUpdatedEvent for room: {}", event.getRoomId());
        kafkaTemplate.send(KafkaTopics.ROOM_EVENTS, event.getRoomId().toString(), event);
    }

    public void publishRoomDeleted(RoomDeletedEvent event) {
        log.info("Publishing RoomDeletedEvent for room: {}", event.getRoomId());
        kafkaTemplate.send(KafkaTopics.ROOM_EVENTS, event.getRoomId().toString(), event);
    }

    public void publishStoryCreated(StoryCreatedEvent event) {
        log.info("Publishing StoryCreatedEvent for story: {}", event.getStoryId());
        kafkaTemplate.send(KafkaTopics.STORY_EVENTS, event.getRoomId().toString(), event);
    }

    public void publishStoryUpdated(StoryUpdatedEvent event) {
        log.info("Publishing StoryUpdatedEvent for story: {}", event.getStoryId());
        kafkaTemplate.send(KafkaTopics.STORY_EVENTS, event.getRoomId().toString(), event);
    }

    public void publishStoryDeleted(StoryDeletedEvent event) {
        log.info("Publishing StoryDeletedEvent for story: {}", event.getStoryId());
        kafkaTemplate.send(KafkaTopics.STORY_EVENTS, event.getRoomId().toString(), event);
    }

    public void publishVotingStarted(VotingStartedEvent event) {
        log.info("Publishing VotingStartedEvent for story: {}", event.getStoryId());
        kafkaTemplate.send(KafkaTopics.STORY_EVENTS, event.getRoomId().toString(), event);
    }
}

