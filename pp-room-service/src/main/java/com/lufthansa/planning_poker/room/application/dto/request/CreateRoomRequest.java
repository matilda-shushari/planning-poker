package com.lufthansa.planning_poker.room.application.dto.request;

import com.lufthansa.planning_poker.room.domain.model.DeckType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateRoomRequest(
    @NotBlank(message = "Room name is required")
    @Size(min = 3, max = 100, message = "Room name must be between 3 and 100 characters")
    String name,

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    String description,

    @NotNull(message = "Deck type is required")
    DeckType deckType,

    List<String> customDeckValues
) {}

