package com.lufthansa.planning_poker.room.application.dto.response;

import com.lufthansa.planning_poker.room.domain.model.DeckType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoomResponse(
    UUID id,
    String name,
    String description,
    DeckType deckType,
    List<String> deckValues,
    String moderatorId,
    String moderatorName,
    String shortCode,
    String inviteLink,
    boolean active,
    Instant createdAt,
    Instant updatedAt,
    int participantCount,
    int storyCount,
    List<ParticipantResponse> participants,
    List<StoryResponse> stories
) {
    public static RoomResponse withoutDetails(
        UUID id, String name, String description, DeckType deckType,
        List<String> deckValues, String moderatorId, String moderatorName,
        String shortCode, boolean active, Instant createdAt, Instant updatedAt,
        int participantCount, int storyCount
    ) {
        return new RoomResponse(
            id, name, description, deckType, deckValues, moderatorId, moderatorName,
            shortCode, generateInviteLink(shortCode), active, createdAt, updatedAt,
            participantCount, storyCount, null, null
        );
    }

    private static String generateInviteLink(String shortCode) {
        return "/join/" + shortCode;
    }
}

