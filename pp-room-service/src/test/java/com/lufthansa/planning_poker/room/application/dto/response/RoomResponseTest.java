package com.lufthansa.planning_poker.room.application.dto.response;

import com.lufthansa.planning_poker.room.domain.model.DeckType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RoomResponse Tests")
class RoomResponseTest {

    @Nested
    @DisplayName("Record Creation")
    class RecordCreation {

        @Test
        @DisplayName("Should create RoomResponse with all fields")
        void shouldCreateWithAllFields() {
            // Given
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();
            List<String> deckValues = Arrays.asList("1", "2", "3", "5", "8");
            List<ParticipantResponse> participants = Collections.emptyList();
            List<StoryResponse> stories = Collections.emptyList();

            // When
            RoomResponse response = new RoomResponse(
                id, "Sprint Planning", "Planning for Sprint 42",
                DeckType.FIBONACCI, deckValues, "mod-123", "John Moderator",
                "ABC123", "/join/ABC123", true, now, now,
                5, 3, participants, stories
            );

            // Then
            assertThat(response.id()).isEqualTo(id);
            assertThat(response.name()).isEqualTo("Sprint Planning");
            assertThat(response.description()).isEqualTo("Planning for Sprint 42");
            assertThat(response.deckType()).isEqualTo(DeckType.FIBONACCI);
            assertThat(response.deckValues()).hasSize(5);
            assertThat(response.moderatorId()).isEqualTo("mod-123");
            assertThat(response.moderatorName()).isEqualTo("John Moderator");
            assertThat(response.shortCode()).isEqualTo("ABC123");
            assertThat(response.inviteLink()).isEqualTo("/join/ABC123");
            assertThat(response.active()).isTrue();
            assertThat(response.participantCount()).isEqualTo(5);
            assertThat(response.storyCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should handle null optional fields")
        void shouldHandleNullOptionalFields() {
            // When
            RoomResponse response = new RoomResponse(
                UUID.randomUUID(), "Room", null,
                DeckType.SCRUM, null, "mod-1", null,
                "XYZ", "/join/XYZ", true, Instant.now(), null,
                0, 0, null, null
            );

            // Then
            assertThat(response.description()).isNull();
            assertThat(response.deckValues()).isNull();
            assertThat(response.moderatorName()).isNull();
            assertThat(response.updatedAt()).isNull();
            assertThat(response.participants()).isNull();
            assertThat(response.stories()).isNull();
        }
    }

    @Nested
    @DisplayName("withoutDetails Static Method")
    class WithoutDetailsMethod {

        @Test
        @DisplayName("Should create response without participants and stories")
        void shouldCreateWithoutDetails() {
            // Given
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();
            List<String> deckValues = Arrays.asList("XS", "S", "M", "L", "XL");

            // When
            RoomResponse response = RoomResponse.withoutDetails(
                id, "T-Shirt Room", "Planning with t-shirt sizes",
                DeckType.TSHIRT, deckValues, "mod-1", "Moderator",
                "TSH123", true, now, now, 4, 2
            );

            // Then
            assertThat(response.id()).isEqualTo(id);
            assertThat(response.name()).isEqualTo("T-Shirt Room");
            assertThat(response.description()).isEqualTo("Planning with t-shirt sizes");
            assertThat(response.deckType()).isEqualTo(DeckType.TSHIRT);
            assertThat(response.inviteLink()).isEqualTo("/join/TSH123");
            assertThat(response.participantCount()).isEqualTo(4);
            assertThat(response.storyCount()).isEqualTo(2);
            assertThat(response.participants()).isNull();
            assertThat(response.stories()).isNull();
        }

        @Test
        @DisplayName("Should generate correct invite link")
        void shouldGenerateCorrectInviteLink() {
            // Given
            String shortCode = "TEST456";

            // When
            RoomResponse response = RoomResponse.withoutDetails(
                UUID.randomUUID(), "Test", "Test Desc",
                DeckType.SEQUENTIAL, Collections.emptyList(), "mod", "Mod",
                shortCode, true, Instant.now(), Instant.now(), 0, 0
            );

            // Then
            assertThat(response.inviteLink()).isEqualTo("/join/TEST456");
        }

        @Test
        @DisplayName("Should handle different deck types")
        void shouldHandleDifferentDeckTypes() {
            Instant now = Instant.now();
            
            for (DeckType deckType : DeckType.values()) {
                RoomResponse response = RoomResponse.withoutDetails(
                    UUID.randomUUID(), "Room", "Desc",
                    deckType, Collections.emptyList(), "mod", "Mod",
                    "CODE", true, now, now, 0, 0
                );
                
                assertThat(response.deckType()).isEqualTo(deckType);
            }
        }
    }

    @Nested
    @DisplayName("Record Equality")
    class RecordEquality {

        @Test
        @DisplayName("Should be equal when all fields match")
        void shouldBeEqualWhenFieldsMatch() {
            // Given
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();

            RoomResponse response1 = new RoomResponse(
                id, "Room", "Desc", DeckType.FIBONACCI, List.of("1", "2"),
                "mod", "Mod", "ABC", "/join/ABC", true, now, now,
                1, 1, Collections.emptyList(), Collections.emptyList()
            );

            RoomResponse response2 = new RoomResponse(
                id, "Room", "Desc", DeckType.FIBONACCI, List.of("1", "2"),
                "mod", "Mod", "ABC", "/join/ABC", true, now, now,
                1, 1, Collections.emptyList(), Collections.emptyList()
            );

            // Then
            assertThat(response1).isEqualTo(response2);
            assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when fields differ")
        void shouldNotBeEqualWhenFieldsDiffer() {
            // Given
            Instant now = Instant.now();

            RoomResponse response1 = new RoomResponse(
                UUID.randomUUID(), "Room1", "Desc", DeckType.FIBONACCI, List.of("1"),
                "mod", "Mod", "ABC", "/join/ABC", true, now, now,
                1, 1, null, null
            );

            RoomResponse response2 = new RoomResponse(
                UUID.randomUUID(), "Room2", "Desc", DeckType.FIBONACCI, List.of("1"),
                "mod", "Mod", "ABC", "/join/ABC", true, now, now,
                1, 1, null, null
            );

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }
    }
}

