package com.lufthansa.planning_poker.room.application.service;

import com.lufthansa.planning_poker.room.api.exception.BusinessException;
import com.lufthansa.planning_poker.room.application.dto.request.CreateStoryRequest;
import com.lufthansa.planning_poker.room.application.dto.request.UpdateStoryRequest;
import com.lufthansa.planning_poker.room.application.dto.response.StoryResponse;
import com.lufthansa.planning_poker.room.application.mapper.RoomMapper;
import com.lufthansa.planning_poker.room.domain.model.DeckType;
import com.lufthansa.planning_poker.room.domain.model.StoryStatus;
import com.lufthansa.planning_poker.room.infrastructure.messaging.RoomEventProducer;
import com.lufthansa.planning_poker.room.infrastructure.persistence.entity.RoomEntity;
import com.lufthansa.planning_poker.room.infrastructure.persistence.entity.StoryEntity;
import com.lufthansa.planning_poker.room.infrastructure.persistence.repository.JpaRoomRepository;
import com.lufthansa.planning_poker.room.infrastructure.persistence.repository.JpaStoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoryService Tests")
class StoryServiceTest {

    @Mock
    private JpaStoryRepository storyRepository;

    @Mock
    private JpaRoomRepository roomRepository;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private RoomEventProducer eventProducer;

    @InjectMocks
    private StoryService storyService;

    private UUID roomId;
    private UUID storyId;
    private String moderatorId;
    private String moderatorName;
    private RoomEntity roomEntity;
    private StoryEntity storyEntity;
    private StoryResponse storyResponse;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        storyId = UUID.randomUUID();
        moderatorId = "user-123";
        moderatorName = "testuser";

        roomEntity = RoomEntity.builder()
            .id(roomId)
            .name("Sprint 42 Planning")
            .deckType(DeckType.FIBONACCI)
            .moderatorId(moderatorId)
            .moderatorName(moderatorName)
            .shortCode("ABC123")
            .active(true)
            .build();

        storyEntity = StoryEntity.builder()
            .id(storyId)
            .room(roomEntity)
            .title("User Authentication")
            .description("Implement OAuth2 login")
            .jiraLink("https://jira.example.com/PROJ-101")
            .status(StoryStatus.PENDING)
            .displayOrder(1)
            .createdAt(Instant.now())
            .build();

        storyResponse = new StoryResponse(
            storyId, roomId, "User Authentication",
            "Implement OAuth2 login", "https://jira.example.com/PROJ-101",
            StoryStatus.PENDING, null, null, 1, Instant.now(), null, null, null
        );
    }

    @Nested
    @DisplayName("createStory Tests")
    class CreateStoryTests {

        @Test
        @DisplayName("Should create story successfully")
        void shouldCreateStorySuccessfully() {
            // Given
            CreateStoryRequest request = new CreateStoryRequest(
                "User Authentication",
                "Implement OAuth2 login",
                "https://jira.example.com/PROJ-101"
            );

            when(roomRepository.findById(roomId)).thenReturn(Optional.of(roomEntity));
            when(storyRepository.getNextDisplayOrder(roomId)).thenReturn(1);
            when(storyRepository.save(any(StoryEntity.class))).thenReturn(storyEntity);
            when(roomMapper.toStoryResponse(storyEntity)).thenReturn(storyResponse);

            // When
            StoryResponse result = storyService.createStory(roomId, request, moderatorId, moderatorName);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.title()).isEqualTo("User Authentication");
            verify(storyRepository).save(any(StoryEntity.class));
            verify(eventProducer).publishStoryCreated(any());
        }

        @Test
        @DisplayName("Should create story with PENDING status")
        void shouldCreateStoryWithPendingStatus() {
            // Given
            CreateStoryRequest request = new CreateStoryRequest("Title", "Description", null);

            when(roomRepository.findById(roomId)).thenReturn(Optional.of(roomEntity));
            when(storyRepository.getNextDisplayOrder(roomId)).thenReturn(1);
            when(storyRepository.save(any(StoryEntity.class))).thenReturn(storyEntity);
            when(roomMapper.toStoryResponse(any())).thenReturn(storyResponse);

            // When
            storyService.createStory(roomId, request, moderatorId, moderatorName);

            // Then
            ArgumentCaptor<StoryEntity> storyCaptor = ArgumentCaptor.forClass(StoryEntity.class);
            verify(storyRepository).save(storyCaptor.capture());
            assertThat(storyCaptor.getValue().getStatus()).isEqualTo(StoryStatus.PENDING);
        }

        @Test
        @DisplayName("Should throw BusinessException when non-moderator creates story")
        void shouldThrowWhenNonModeratorCreatesStory() {
            // Given
            CreateStoryRequest request = new CreateStoryRequest("Title", "Description", null);
            String differentUserId = "other-user";

            when(roomRepository.findById(roomId)).thenReturn(Optional.of(roomEntity));

            // When/Then
            assertThatThrownBy(() -> storyService.createStory(roomId, request, differentUserId, "other"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("moderator");
        }

        @Test
        @DisplayName("Should throw BusinessException when room not found")
        void shouldThrowWhenRoomNotFound() {
            // Given
            UUID nonExistentRoomId = UUID.randomUUID();
            CreateStoryRequest request = new CreateStoryRequest("Title", "Description", null);

            when(roomRepository.findById(nonExistentRoomId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> storyService.createStory(nonExistentRoomId, request, moderatorId, moderatorName))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Should set correct display order for new story")
        void shouldSetCorrectDisplayOrder() {
            // Given
            CreateStoryRequest request = new CreateStoryRequest("Title", "Description", null);

            when(roomRepository.findById(roomId)).thenReturn(Optional.of(roomEntity));
            when(storyRepository.getNextDisplayOrder(roomId)).thenReturn(5);
            when(storyRepository.save(any(StoryEntity.class))).thenReturn(storyEntity);
            when(roomMapper.toStoryResponse(any())).thenReturn(storyResponse);

            // When
            storyService.createStory(roomId, request, moderatorId, moderatorName);

            // Then
            ArgumentCaptor<StoryEntity> storyCaptor = ArgumentCaptor.forClass(StoryEntity.class);
            verify(storyRepository).save(storyCaptor.capture());
            assertThat(storyCaptor.getValue().getDisplayOrder()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("getStoryById Tests")
    class GetStoryByIdTests {

        @Test
        @DisplayName("Should return story when found")
        void shouldReturnStoryWhenFound() {
            // Given
            when(storyRepository.findById(storyId)).thenReturn(Optional.of(storyEntity));
            when(roomMapper.toStoryResponse(storyEntity)).thenReturn(storyResponse);

            // When
            StoryResponse result = storyService.getStoryById(storyId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(storyId);
        }

        @Test
        @DisplayName("Should throw BusinessException when story not found")
        void shouldThrowWhenStoryNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(storyRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> storyService.getStoryById(nonExistentId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("getStoriesByRoomId Tests")
    class GetStoriesByRoomIdTests {

        @Test
        @DisplayName("Should return all stories for room ordered by display order")
        void shouldReturnAllStoriesForRoom() {
            // Given
            StoryEntity story2 = StoryEntity.builder()
                .id(UUID.randomUUID())
                .room(roomEntity)
                .title("Second Story")
                .status(StoryStatus.PENDING)
                .displayOrder(2)
                .build();

            List<StoryEntity> stories = List.of(storyEntity, story2);
            when(storyRepository.findByRoomIdOrderByDisplayOrderAsc(roomId)).thenReturn(stories);
            when(roomMapper.toStoryResponse(any())).thenReturn(storyResponse);

            // When
            List<StoryResponse> result = storyService.getStoriesByRoomId(roomId);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list when no stories")
        void shouldReturnEmptyListWhenNoStories() {
            // Given
            when(storyRepository.findByRoomIdOrderByDisplayOrderAsc(roomId)).thenReturn(List.of());

            // When
            List<StoryResponse> result = storyService.getStoriesByRoomId(roomId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateStory Tests")
    class UpdateStoryTests {

        @Test
        @DisplayName("Should update story successfully")
        void shouldUpdateStorySuccessfully() {
            // Given
            UpdateStoryRequest request = new UpdateStoryRequest(
                "Updated Title",
                "Updated Description",
                "https://jira.example.com/PROJ-102"
            );

            when(storyRepository.findById(storyId)).thenReturn(Optional.of(storyEntity));
            when(storyRepository.save(any(StoryEntity.class))).thenReturn(storyEntity);
            when(roomMapper.toStoryResponse(any())).thenReturn(storyResponse);

            // When
            StoryResponse result = storyService.updateStory(storyId, request, moderatorId, moderatorName);

            // Then
            assertThat(result).isNotNull();
            verify(storyRepository).save(any(StoryEntity.class));
            verify(eventProducer).publishStoryUpdated(any());
        }

        @Test
        @DisplayName("Should throw BusinessException when non-moderator updates story")
        void shouldThrowWhenNonModeratorUpdatesStory() {
            // Given
            UpdateStoryRequest request = new UpdateStoryRequest("Title", "Description", null);
            String differentUserId = "other-user";

            when(storyRepository.findById(storyId)).thenReturn(Optional.of(storyEntity));

            // When/Then
            assertThatThrownBy(() -> storyService.updateStory(storyId, request, differentUserId, "other"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("moderator");
        }

        @Test
        @DisplayName("Should throw BusinessException when updating completed story")
        void shouldThrowWhenUpdatingCompletedStory() {
            // Given
            storyEntity.setStatus(StoryStatus.COMPLETED);
            UpdateStoryRequest request = new UpdateStoryRequest("Title", "Description", null);

            when(storyRepository.findById(storyId)).thenReturn(Optional.of(storyEntity));

            // When/Then
            assertThatThrownBy(() -> storyService.updateStory(storyId, request, moderatorId, moderatorName))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("completed");
        }
    }

    @Nested
    @DisplayName("deleteStory Tests")
    class DeleteStoryTests {

        @Test
        @DisplayName("Should delete story when user is moderator")
        void shouldDeleteStoryWhenModerator() {
            // Given
            when(storyRepository.findById(storyId)).thenReturn(Optional.of(storyEntity));

            // When
            storyService.deleteStory(storyId, moderatorId, moderatorName, false);

            // Then
            verify(storyRepository).delete(storyEntity);
            verify(eventProducer).publishStoryDeleted(any());
        }

        @Test
        @DisplayName("Should delete story when user is admin")
        void shouldDeleteStoryWhenAdmin() {
            // Given
            String adminId = "admin-user";
            when(storyRepository.findById(storyId)).thenReturn(Optional.of(storyEntity));

            // When
            storyService.deleteStory(storyId, adminId, "admin", true);

            // Then
            verify(storyRepository).delete(storyEntity);
        }

        @Test
        @DisplayName("Should throw BusinessException when non-moderator non-admin deletes")
        void shouldThrowWhenUnauthorizedDelete() {
            // Given
            String randomUserId = "random-user";
            when(storyRepository.findById(storyId)).thenReturn(Optional.of(storyEntity));

            // When/Then
            assertThatThrownBy(() -> storyService.deleteStory(storyId, randomUserId, "random", false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("moderator");
        }

        @Test
        @DisplayName("Should throw BusinessException when deleting story during voting")
        void shouldThrowWhenDeletingDuringVoting() {
            // Given
            storyEntity.setStatus(StoryStatus.VOTING);
            when(storyRepository.findById(storyId)).thenReturn(Optional.of(storyEntity));

            // When/Then
            assertThatThrownBy(() -> storyService.deleteStory(storyId, moderatorId, moderatorName, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("voting is in progress");
        }
    }

    @Nested
    @DisplayName("startVoting Tests")
    class StartVotingTests {

        @Test
        @DisplayName("Should start voting successfully")
        void shouldStartVotingSuccessfully() {
            // Given
            when(storyRepository.findById(storyId)).thenReturn(Optional.of(storyEntity));
            when(storyRepository.findActiveVotingStory(roomId)).thenReturn(Optional.empty());
            when(storyRepository.save(any(StoryEntity.class))).thenReturn(storyEntity);
            when(roomMapper.toStoryResponse(any())).thenReturn(storyResponse);

            // When
            StoryResponse result = storyService.startVoting(storyId, moderatorId, moderatorName);

            // Then
            assertThat(result).isNotNull();
            ArgumentCaptor<StoryEntity> storyCaptor = ArgumentCaptor.forClass(StoryEntity.class);
            verify(storyRepository).save(storyCaptor.capture());
            assertThat(storyCaptor.getValue().getStatus()).isEqualTo(StoryStatus.VOTING);
        }

        @Test
        @DisplayName("Should throw BusinessException when non-moderator starts voting")
        void shouldThrowWhenNonModeratorStartsVoting() {
            // Given
            String differentUserId = "other-user";
            when(storyRepository.findById(storyId)).thenReturn(Optional.of(storyEntity));

            // When/Then
            assertThatThrownBy(() -> storyService.startVoting(storyId, differentUserId, "other"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("moderator");
        }

        @Test
        @DisplayName("Should throw BusinessException when another story is already in voting")
        void shouldThrowWhenAnotherStoryIsVoting() {
            // Given
            StoryEntity activeStory = StoryEntity.builder()
                .id(UUID.randomUUID())
                .title("Active Story")
                .status(StoryStatus.VOTING)
                .build();

            when(storyRepository.findById(storyId)).thenReturn(Optional.of(storyEntity));
            when(storyRepository.findActiveVotingStory(roomId)).thenReturn(Optional.of(activeStory));

            // When/Then
            assertThatThrownBy(() -> storyService.startVoting(storyId, moderatorId, moderatorName))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Another story is already in voting");
        }

        @Test
        @DisplayName("Should allow restarting voting on same story")
        void shouldAllowRestartingVotingOnSameStory() {
            // Given
            storyEntity.setStatus(StoryStatus.VOTING);
            when(storyRepository.findById(storyId)).thenReturn(Optional.of(storyEntity));
            when(storyRepository.findActiveVotingStory(roomId)).thenReturn(Optional.of(storyEntity));
            when(storyRepository.save(any(StoryEntity.class))).thenReturn(storyEntity);
            when(roomMapper.toStoryResponse(any())).thenReturn(storyResponse);

            // When
            StoryResponse result = storyService.startVoting(storyId, moderatorId, moderatorName);

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should set votingStartedAt timestamp")
        void shouldSetVotingStartedAtTimestamp() {
            // Given
            when(storyRepository.findById(storyId)).thenReturn(Optional.of(storyEntity));
            when(storyRepository.findActiveVotingStory(roomId)).thenReturn(Optional.empty());
            when(storyRepository.save(any(StoryEntity.class))).thenReturn(storyEntity);
            when(roomMapper.toStoryResponse(any())).thenReturn(storyResponse);

            // When
            storyService.startVoting(storyId, moderatorId, moderatorName);

            // Then
            ArgumentCaptor<StoryEntity> storyCaptor = ArgumentCaptor.forClass(StoryEntity.class);
            verify(storyRepository).save(storyCaptor.capture());
            assertThat(storyCaptor.getValue().getVotingStartedAt()).isNotNull();
        }
    }
}

