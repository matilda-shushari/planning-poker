package com.lufthansa.planning_poker.audit.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lufthansa.planning_poker.audit.infrastructure.persistence.entity.AuditLogEntity;
import com.lufthansa.planning_poker.audit.infrastructure.persistence.repository.JpaAuditLogRepository;
import com.lufthansa.planning_poker.common.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventConsumer {

    private final JpaAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.ROOM_EVENTS, groupId = KafkaTopics.AUDIT_CONSUMER_GROUP)
    public void consumeRoomEvents(BaseEvent event) {
        log.info("Received room event: {}", event.getEventType());
        processEvent(event, "ROOM", determineRoomAction(event));
    }

    @KafkaListener(topics = KafkaTopics.STORY_EVENTS, groupId = KafkaTopics.AUDIT_CONSUMER_GROUP)
    public void consumeStoryEvents(BaseEvent event) {
        log.info("Received story event: {}", event.getEventType());
        processEvent(event, "STORY", determineStoryAction(event));
    }

    @KafkaListener(topics = KafkaTopics.VOTE_EVENTS, groupId = KafkaTopics.AUDIT_CONSUMER_GROUP)
    public void consumeVoteEvents(BaseEvent event) {
        log.info("Received vote event: {}", event.getEventType());
        processEvent(event, "VOTE", determineVoteAction(event));
    }

    private void processEvent(BaseEvent event, String entityType, AuditLogEntity.AuditAction action) {
        try {
            String entityId = extractEntityId(event);
            String eventData = objectMapper.writeValueAsString(event);

            AuditLogEntity auditLog = AuditLogEntity.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .eventData(eventData)
                .userId(event.getTriggeredBy())
                .userName(event.getTriggeredByName())
                .timestamp(event.getTimestamp())
                .sourceService(determineSourceService(entityType))
                .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved for event: {}", event.getEventId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event data", e);
        } catch (Exception e) {
            log.error("Failed to process audit event", e);
        }
    }

    private String extractEntityId(BaseEvent event) {
        if (event instanceof RoomCreatedEvent e) return e.getRoomId().toString();
        if (event instanceof RoomUpdatedEvent e) return e.getRoomId().toString();
        if (event instanceof RoomDeletedEvent e) return e.getRoomId().toString();
        if (event instanceof StoryCreatedEvent e) return e.getStoryId().toString();
        if (event instanceof StoryUpdatedEvent e) return e.getStoryId().toString();
        if (event instanceof StoryDeletedEvent e) return e.getStoryId().toString();
        if (event instanceof VoteCastEvent e) return e.getStoryId().toString();
        if (event instanceof VotingStartedEvent e) return e.getStoryId().toString();
        if (event instanceof VotingFinishedEvent e) return e.getStoryId().toString();
        return "unknown";
    }

    private AuditLogEntity.AuditAction determineRoomAction(BaseEvent event) {
        if (event instanceof RoomCreatedEvent) return AuditLogEntity.AuditAction.CREATE;
        if (event instanceof RoomUpdatedEvent) return AuditLogEntity.AuditAction.UPDATE;
        if (event instanceof RoomDeletedEvent) return AuditLogEntity.AuditAction.DELETE;
        return AuditLogEntity.AuditAction.UPDATE;
    }

    private AuditLogEntity.AuditAction determineStoryAction(BaseEvent event) {
        if (event instanceof StoryCreatedEvent) return AuditLogEntity.AuditAction.CREATE;
        if (event instanceof StoryUpdatedEvent) return AuditLogEntity.AuditAction.UPDATE;
        if (event instanceof StoryDeletedEvent) return AuditLogEntity.AuditAction.DELETE;
        return AuditLogEntity.AuditAction.UPDATE;
    }

    private AuditLogEntity.AuditAction determineVoteAction(BaseEvent event) {
        if (event instanceof VoteCastEvent) return AuditLogEntity.AuditAction.VOTE;
        if (event instanceof VotingStartedEvent) return AuditLogEntity.AuditAction.START;
        if (event instanceof VotingFinishedEvent) return AuditLogEntity.AuditAction.FINISH;
        return AuditLogEntity.AuditAction.UPDATE;
    }

    private String determineSourceService(String entityType) {
        return switch (entityType) {
            case "ROOM", "STORY" -> "pp-room-service";
            case "VOTE" -> "pp-vote-service";
            default -> "unknown";
        };
    }
}

