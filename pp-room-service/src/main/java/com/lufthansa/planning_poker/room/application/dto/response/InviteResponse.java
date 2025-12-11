package com.lufthansa.planning_poker.room.application.dto.response;

import java.time.Instant;
import java.util.UUID;

public record InviteResponse(
    UUID id,
    UUID roomId,
    String roomName,
    String email,
    String status,
    String inviteLink,
    Instant createdAt,
    Instant expiresAt
) {}

