package com.lufthansa.planning_poker.vote.application.dto.response;

import java.time.Instant;
import java.util.UUID;

public record VoteResponse(
    UUID id,
    UUID storyId,
    UUID roomId,
    String userId,
    String userName,
    String value,
    Instant createdAt,
    Instant updatedAt
) {}

