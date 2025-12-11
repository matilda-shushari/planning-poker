package com.lufthansa.planning_poker.room.application.dto.response;

import com.lufthansa.planning_poker.room.domain.model.StoryStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StoryResponse(
    UUID id,
    UUID roomId,
    String title,
    String description,
    String jiraLink,
    StoryStatus status,
    BigDecimal averageScore,
    String finalEstimate,
    Integer displayOrder,
    Instant createdAt,
    Instant updatedAt,
    Instant votingStartedAt,
    Instant votingEndedAt
) {}

