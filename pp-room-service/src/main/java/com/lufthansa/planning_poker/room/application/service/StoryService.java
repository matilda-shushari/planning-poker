package com.lufthansa.planning_poker.room.application.service;

import com.lufthansa.planning_poker.common.event.StoryCreatedEvent;
import com.lufthansa.planning_poker.common.event.StoryDeletedEvent;
import com.lufthansa.planning_poker.common.event.StoryUpdatedEvent;
import com.lufthansa.planning_poker.common.event.VotingStartedEvent;
import com.lufthansa.planning_poker.room.application.dto.request.CreateStoryRequest;
import com.lufthansa.planning_poker.room.application.dto.request.UpdateStoryRequest;
import com.lufthansa.planning_poker.room.application.dto.response.StoryResponse;
import com.lufthansa.planning_poker.room.application.mapper.RoomMapper;
import com.lufthansa.planning_poker.room.domain.model.StoryStatus;
import com.lufthansa.planning_poker.room.infrastructure.messaging.RoomEventProducer;
import com.lufthansa.planning_poker.room.infrastructure.persistence.entity.RoomEntity;
import com.lufthansa.planning_poker.room.infrastructure.persistence.entity.StoryEntity;
import com.lufthansa.planning_poker.room.infrastructure.persistence.repository.JpaRoomRepository;
import com.lufthansa.planning_poker.room.infrastructure.persistence.repository.JpaStoryRepository;
import com.lufthansa.planning_poker.room.api.exception.BusinessException;
import com.lufthansa.planning_poker.room.application.constants.RoomServiceConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing stories within Planning Poker rooms.
 * <p>
 * Handles story CRUD operations and voting lifecycle management.
 * Only room moderators can create, update, or delete stories.
 * </p>
 *
 * @author Matilda Dervishaj
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StoryService {

    private final JpaStoryRepository storyRepository;
    private final JpaRoomRepository roomRepository;
    private final RoomMapper roomMapper;
    private final RoomEventProducer eventProducer;

    /**
     * Creates a new story in a room.
     *
     * @param roomId   the room to add the story to
     * @param request  the story creation request
     * @param userId   the user creating the story (must be moderator)
     * @param userName the user's display name
     * @return the created story details
     * @throws BusinessException if room not found or user is not moderator
     */
    public StoryResponse createStory(UUID roomId, CreateStoryRequest request, String userId, String userName) {
        RoomEntity room = roomRepository.findById(roomId)
            .orElseThrow(() -> BusinessException.notFound(RoomServiceConstants.ENTITY_ROOM, roomId));

        if (!room.getModeratorId().equals(userId)) {
            throw BusinessException.forbidden(RoomServiceConstants.ERR_ONLY_MODERATOR_CREATE_STORY);
        }

        Integer nextOrder = storyRepository.getNextDisplayOrder(roomId);

        StoryEntity story = StoryEntity.builder()
            .room(room)
            .title(request.title())
            .description(request.description())
            .jiraLink(request.jiraLink())
            .status(StoryStatus.PENDING)
            .displayOrder(nextOrder)
            .build();

        StoryEntity saved = storyRepository.save(story);

        // Publish event
        StoryCreatedEvent event = StoryCreatedEvent.builder()
            .storyId(saved.getId())
            .roomId(roomId)
            .title(saved.getTitle())
            .description(saved.getDescription())
            .jiraLink(saved.getJiraLink())
            .displayOrder(saved.getDisplayOrder())
            .build();
        event.initialize(userId, userName);
        eventProducer.publishStoryCreated(event);

        log.info("Story '{}' created in room {}", saved.getTitle(), roomId);
        return roomMapper.toStoryResponse(saved);
    }

    @Transactional(readOnly = true)
    public StoryResponse getStoryById(UUID storyId) {
        StoryEntity story = storyRepository.findById(storyId)
            .orElseThrow(() -> BusinessException.notFound(RoomServiceConstants.ENTITY_STORY, storyId));
        return roomMapper.toStoryResponse(story);
    }

    @Transactional(readOnly = true)
    public List<StoryResponse> getStoriesByRoomId(UUID roomId) {
        return storyRepository.findByRoomIdOrderByDisplayOrderAsc(roomId)
            .stream()
            .map(roomMapper::toStoryResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<StoryResponse> getAllStories(Pageable pageable) {
        return storyRepository.findAllWithRoom(pageable)
            .map(roomMapper::toStoryResponse);
    }

    public StoryResponse updateStory(UUID storyId, UpdateStoryRequest request, String userId, String userName) {
        StoryEntity story = storyRepository.findById(storyId)
            .orElseThrow(() -> BusinessException.notFound(RoomServiceConstants.ENTITY_STORY, storyId));

        if (!story.getRoom().getModeratorId().equals(userId)) {
            throw BusinessException.forbidden(RoomServiceConstants.ERR_ONLY_MODERATOR_UPDATE_STORY);
        }

        if (story.getStatus() == StoryStatus.COMPLETED) {
            throw BusinessException.conflict(RoomServiceConstants.ERR_CANNOT_UPDATE_COMPLETED);
        }

        String previousTitle = story.getTitle();
        story.setTitle(request.title());
        story.setDescription(request.description());
        story.setJiraLink(request.jiraLink());

        StoryEntity saved = storyRepository.save(story);

        // Publish event
        StoryUpdatedEvent event = StoryUpdatedEvent.builder()
            .storyId(saved.getId())
            .roomId(saved.getRoom().getId())
            .title(saved.getTitle())
            .description(saved.getDescription())
            .jiraLink(saved.getJiraLink())
            .previousTitle(previousTitle)
            .build();
        event.initialize(userId, userName);
        eventProducer.publishStoryUpdated(event);

        log.info("Story {} updated", storyId);
        return roomMapper.toStoryResponse(saved);
    }

    public void deleteStory(UUID storyId, String userId, String userName, boolean isAdmin) {
        StoryEntity story = storyRepository.findById(storyId)
            .orElseThrow(() -> BusinessException.notFound(RoomServiceConstants.ENTITY_STORY, storyId));

        if (!isAdmin && !story.getRoom().getModeratorId().equals(userId)) {
            throw BusinessException.forbidden(RoomServiceConstants.ERR_ONLY_MODERATOR_DELETE_STORY);
        }

        if (story.getStatus() == StoryStatus.VOTING) {
            throw BusinessException.conflict(RoomServiceConstants.ERR_CANNOT_DELETE_DURING_VOTING);
        }

        UUID roomId = story.getRoom().getId();
        String title = story.getTitle();
        storyRepository.delete(story);

        // Publish event
        StoryDeletedEvent event = StoryDeletedEvent.builder()
            .storyId(storyId)
            .roomId(roomId)
            .title(title)
            .build();
        event.initialize(userId, userName);
        eventProducer.publishStoryDeleted(event);

        log.info("Story {} deleted", storyId);
    }

    /**
     * Starts the voting process for a story.
     *
     * @param storyId  the story to start voting on
     * @param userId   the user starting the vote (must be moderator)
     * @param userName the user's display name
     * @return the updated story details
     * @throws BusinessException if another story is already in voting
     */
    public StoryResponse startVoting(UUID storyId, String userId, String userName) {
        StoryEntity story = storyRepository.findById(storyId)
            .orElseThrow(() -> BusinessException.notFound(RoomServiceConstants.ENTITY_STORY, storyId));

        if (!story.getRoom().getModeratorId().equals(userId)) {
            throw BusinessException.forbidden(RoomServiceConstants.ERR_ONLY_MODERATOR_START_VOTING);
        }

        // Check if there's already an active voting story in this room
        storyRepository.findActiveVotingStory(story.getRoom().getId())
            .ifPresent(activeStory -> {
                if (!activeStory.getId().equals(storyId)) {
                    throw BusinessException.conflict("Another story is already in voting: " + activeStory.getTitle());
                }
            });

        story.setStatus(StoryStatus.VOTING);
        story.setVotingStartedAt(java.time.Instant.now());

        StoryEntity saved = storyRepository.save(story);
        log.info("Voting started for story {}", storyId);

        // Publish event for Vote Service to broadcast via WebSocket
        VotingStartedEvent event = VotingStartedEvent.builder()
            .storyId(storyId)
            .roomId(story.getRoom().getId())
            .storyTitle(story.getTitle())
            .build();
        event.initialize(userId, userName);
        eventProducer.publishVotingStarted(event);

        return roomMapper.toStoryResponse(saved);
    }
}

