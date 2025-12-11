package com.lufthansa.planning_poker.common.event;

/**
 * Centralized Kafka topic names for all services.
 * Ensures consistency across producers and consumers.
 */
public final class KafkaTopics {
    
    private KafkaTopics() {
        // Prevent instantiation
    }
    
    // Room Service publishes to these
    public static final String ROOM_EVENTS = "planning-poker.room-events";
    public static final String STORY_EVENTS = "planning-poker.story-events";
    
    // Vote Service publishes to these
    public static final String VOTE_EVENTS = "planning-poker.vote-events";
    
    // Consumer groups
    public static final String AUDIT_CONSUMER_GROUP = "audit-service-group";
    public static final String VOTE_CONSUMER_GROUP = "vote-service-group";
}

