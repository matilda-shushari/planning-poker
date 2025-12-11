package com.lufthansa.planning_poker.room.application.dto.response;

import com.lufthansa.planning_poker.room.domain.model.ParticipantRole;

import java.time.Instant;
import java.util.UUID;

public record ParticipantResponse(
    UUID id,
    String userId,
    String userName,
    String userEmail,
    ParticipantRole role,
    boolean online,
    Instant joinedAt,
    Instant lastSeenAt
) {}

