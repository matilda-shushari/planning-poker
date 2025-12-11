package com.lufthansa.planning_poker.vote.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CastVoteRequest(
    @NotNull(message = "Story ID is required")
    UUID storyId,
    
    @NotNull(message = "Room ID is required")
    UUID roomId,
    
    @NotBlank(message = "Vote value is required")
    @Size(max = 20, message = "Vote value cannot exceed 20 characters")
    String value
) {}

