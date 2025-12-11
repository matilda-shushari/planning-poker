package com.lufthansa.planning_poker.common.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public abstract class BaseEvent {
    
    private UUID eventId;
    private Instant timestamp;
    private String triggeredBy;
    private String triggeredByName;
    private String eventType;
    
    public void initialize(String userId, String userName) {
        this.eventId = UUID.randomUUID();
        this.timestamp = Instant.now();
        this.triggeredBy = userId;
        this.triggeredByName = userName;
        this.eventType = this.getClass().getSimpleName();
    }
}

