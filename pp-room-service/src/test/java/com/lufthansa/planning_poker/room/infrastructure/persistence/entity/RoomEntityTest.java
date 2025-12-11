package com.lufthansa.planning_poker.room.infrastructure.persistence.entity;

import com.lufthansa.planning_poker.room.domain.model.DeckType;
import com.lufthansa.planning_poker.room.domain.model.ParticipantRole;
import com.lufthansa.planning_poker.room.domain.model.StoryStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RoomEntity Tests")
class RoomEntityTest {

    @Nested
    @DisplayName("addStory")
    class AddStory {

        @Test
        @DisplayName("Should add story to room and set bidirectional relationship")
        void shouldAddStoryToRoom() {
            // Given
            RoomEntity room = createRoom();
            StoryEntity story = createStory();

            // When
            room.addStory(story);

            // Then
            assertThat(room.getStories()).contains(story);
            assertThat(story.getRoom()).isEqualTo(room);
        }

        @Test
        @DisplayName("Should add multiple stories to room")
        void shouldAddMultipleStories() {
            // Given
            RoomEntity room = createRoom();
            StoryEntity story1 = createStory("Story 1");
            StoryEntity story2 = createStory("Story 2");
            StoryEntity story3 = createStory("Story 3");

            // When
            room.addStory(story1);
            room.addStory(story2);
            room.addStory(story3);

            // Then
            assertThat(room.getStories()).hasSize(3);
            assertThat(room.getStories()).contains(story1, story2, story3);
        }
    }

    @Nested
    @DisplayName("removeStory")
    class RemoveStory {

        @Test
        @DisplayName("Should remove story from room and clear relationship")
        void shouldRemoveStoryFromRoom() {
            // Given
            RoomEntity room = createRoom();
            StoryEntity story = createStory();
            room.addStory(story);
            assertThat(room.getStories()).contains(story);

            // When
            room.removeStory(story);

            // Then
            assertThat(room.getStories()).doesNotContain(story);
            assertThat(story.getRoom()).isNull();
        }
    }

    @Nested
    @DisplayName("addParticipant")
    class AddParticipant {

        @Test
        @DisplayName("Should add participant to room and set bidirectional relationship")
        void shouldAddParticipantToRoom() {
            // Given
            RoomEntity room = createRoom();
            RoomParticipantEntity participant = createParticipant();

            // When
            room.addParticipant(participant);

            // Then
            assertThat(room.getParticipants()).contains(participant);
            assertThat(participant.getRoom()).isEqualTo(room);
        }

        @Test
        @DisplayName("Should add multiple participants to room")
        void shouldAddMultipleParticipants() {
            // Given
            RoomEntity room = createRoom();
            RoomParticipantEntity participant1 = createParticipant("user-1", "Alice");
            RoomParticipantEntity participant2 = createParticipant("user-2", "Bob");

            // When
            room.addParticipant(participant1);
            room.addParticipant(participant2);

            // Then
            assertThat(room.getParticipants()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Builder and Fields")
    class BuilderAndFields {

        @Test
        @DisplayName("Should create room with all fields using builder")
        void shouldCreateRoomWithBuilder() {
            // Given
            UUID roomId = UUID.randomUUID();
            Instant now = Instant.now();

            // When
            RoomEntity room = RoomEntity.builder()
                    .id(roomId)
                    .name("Sprint Planning")
                    .description("Planning for Sprint 42")
                    .deckType(DeckType.FIBONACCI)
                    .deckValues(Arrays.asList("1", "2", "3", "5", "8"))
                    .moderatorId("mod-123")
                    .moderatorName("John Moderator")
                    .shortCode("ABC123")
                    .active(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            // Then
            assertThat(room.getId()).isEqualTo(roomId);
            assertThat(room.getName()).isEqualTo("Sprint Planning");
            assertThat(room.getDescription()).isEqualTo("Planning for Sprint 42");
            assertThat(room.getDeckType()).isEqualTo(DeckType.FIBONACCI);
            assertThat(room.getDeckValues()).containsExactly("1", "2", "3", "5", "8");
            assertThat(room.getModeratorId()).isEqualTo("mod-123");
            assertThat(room.getModeratorName()).isEqualTo("John Moderator");
            assertThat(room.getShortCode()).isEqualTo("ABC123");
            assertThat(room.isActive()).isTrue();
            assertThat(room.getCreatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should have default values for collections")
        void shouldHaveDefaultCollections() {
            // When
            RoomEntity room = RoomEntity.builder()
                    .id(UUID.randomUUID())
                    .name("Test Room")
                    .deckType(DeckType.SCRUM)
                    .moderatorId("mod-1")
                    .shortCode("XYZ")
                    .build();

            // Then
            assertThat(room.getDeckValues()).isNotNull();
            assertThat(room.getStories()).isNotNull();
            assertThat(room.getParticipants()).isNotNull();
            assertThat(room.isActive()).isTrue(); // Default value
        }

        @Test
        @DisplayName("Should support all deck types")
        void shouldSupportAllDeckTypes() {
            for (DeckType deckType : DeckType.values()) {
                RoomEntity room = RoomEntity.builder()
                        .id(UUID.randomUUID())
                        .name("Room with " + deckType)
                        .deckType(deckType)
                        .moderatorId("mod-1")
                        .shortCode("ABC")
                        .build();
                
                assertThat(room.getDeckType()).isEqualTo(deckType);
            }
        }
    }

    @Nested
    @DisplayName("Setters")
    class Setters {

        @Test
        @DisplayName("Should update room fields using setters")
        void shouldUpdateFields() {
            // Given
            RoomEntity room = createRoom();

            // When
            room.setName("Updated Name");
            room.setDescription("Updated Description");
            room.setActive(false);

            // Then
            assertThat(room.getName()).isEqualTo("Updated Name");
            assertThat(room.getDescription()).isEqualTo("Updated Description");
            assertThat(room.isActive()).isFalse();
        }
    }

    // Helper methods
    private RoomEntity createRoom() {
        return RoomEntity.builder()
                .id(UUID.randomUUID())
                .name("Test Room")
                .description("Test Description")
                .deckType(DeckType.FIBONACCI)
                .moderatorId("moderator-1")
                .moderatorName("Moderator")
                .shortCode("TEST123")
                .active(true)
                .createdAt(Instant.now())
                .stories(new HashSet<>())
                .participants(new HashSet<>())
                .build();
    }

    private StoryEntity createStory() {
        return createStory("Test Story");
    }

    private StoryEntity createStory(String title) {
        return StoryEntity.builder()
                .id(UUID.randomUUID())
                .title(title)
                .status(StoryStatus.PENDING)
                .displayOrder(1)
                .createdAt(Instant.now())
                .build();
    }

    private RoomParticipantEntity createParticipant() {
        return createParticipant("user-1", "User One");
    }

    private RoomParticipantEntity createParticipant(String userId, String userName) {
        return RoomParticipantEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .userName(userName)
                .role(ParticipantRole.VOTER)
                .online(true)
                .joinedAt(Instant.now())
                .build();
    }
}

