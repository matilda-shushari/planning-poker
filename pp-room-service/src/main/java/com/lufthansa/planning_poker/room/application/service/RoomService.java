package com.lufthansa.planning_poker.room.application.service;

import com.lufthansa.planning_poker.common.event.RoomCreatedEvent;
import com.lufthansa.planning_poker.common.event.RoomDeletedEvent;
import com.lufthansa.planning_poker.common.event.RoomUpdatedEvent;
import com.lufthansa.planning_poker.room.application.dto.request.CreateRoomRequest;
import com.lufthansa.planning_poker.room.application.dto.request.UpdateRoomRequest;
import com.lufthansa.planning_poker.room.application.dto.response.RoomResponse;
import com.lufthansa.planning_poker.room.application.mapper.RoomMapper;
import com.lufthansa.planning_poker.room.domain.model.DeckType;
import com.lufthansa.planning_poker.room.domain.model.ParticipantRole;
import com.lufthansa.planning_poker.room.infrastructure.messaging.RoomEventProducer;
import com.lufthansa.planning_poker.room.infrastructure.persistence.entity.RoomEntity;
import com.lufthansa.planning_poker.room.infrastructure.persistence.entity.RoomParticipantEntity;
import com.lufthansa.planning_poker.room.infrastructure.persistence.repository.JpaParticipantRepository;
import com.lufthansa.planning_poker.room.infrastructure.persistence.repository.JpaRoomRepository;
import com.lufthansa.planning_poker.room.api.exception.BusinessException;
import com.lufthansa.planning_poker.room.application.constants.RoomServiceConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * Service for managing Planning Poker rooms.
 * <p>
 * Handles room creation, updates, deletion, and participant management.
 * Publishes domain events to Kafka for audit and cross-service communication.
 * </p>
 *
 * @author Matilda Dervishaj
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RoomService {

    private final JpaRoomRepository roomRepository;
    private final JpaParticipantRepository participantRepository;
    private final RoomMapper roomMapper;
    private final RoomEventProducer eventProducer;
    private final SecureRandom random = new SecureRandom();

    /**
     * Creates a new Planning Poker room.
     *
     * @param request  the room creation request containing name, description, and deck type
     * @param userId   the ID of the user creating the room (becomes moderator)
     * @param userName the display name of the user
     * @return the created room details
     */
    public RoomResponse createRoom(CreateRoomRequest request, String userId, String userName) {
        log.info("Creating room '{}' for user {}", request.name(), userId);

        RoomEntity room = RoomEntity.builder()
            .name(request.name())
            .description(request.description())
            .deckType(request.deckType())
            .deckValues(
                request.deckType() == DeckType.CUSTOM && request.customDeckValues() != null
                    ? request.customDeckValues()
                    : request.deckType().getDefaultValues()
            )
            .moderatorId(userId)
            .moderatorName(userName)
            .shortCode(generateUniqueShortCode())
            .active(true)
            .build();

        RoomEntity saved = roomRepository.save(room);

        // Add creator as moderator participant
        RoomParticipantEntity participant = RoomParticipantEntity.builder()
            .room(saved)
            .userId(userId)
            .userName(userName)
            .role(ParticipantRole.MODERATOR)
            .online(true)
            .joinedAt(Instant.now())
            .build();
        participantRepository.save(participant);

        // Publish event
        RoomCreatedEvent event = RoomCreatedEvent.builder()
            .roomId(saved.getId())
            .roomName(saved.getName())
            .description(saved.getDescription())
            .deckType(saved.getDeckType().name())
            .deckValues(saved.getDeckValues())
            .shortCode(saved.getShortCode())
            .moderatorId(userId)
            .moderatorName(userName)
            .build();
        event.initialize(userId, userName);
        eventProducer.publishRoomCreated(event);

        log.info("Room created with ID: {}", saved.getId());
        return roomMapper.toResponse(saved);
    }

    /**
     * Retrieves a room by its unique identifier.
     *
     * @param id the room UUID
     * @return the room details including participants and stories
     * @throws BusinessException if room not found
     */
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(UUID id) {
        RoomEntity room = roomRepository.findByIdWithDetails(id)
            .orElseThrow(() -> BusinessException.notFound(RoomServiceConstants.ENTITY_ROOM, id));
        return roomMapper.toResponse(room);
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomByShortCode(String shortCode) {
        RoomEntity room = roomRepository.findByShortCodeWithDetails(shortCode.toUpperCase())
            .orElseThrow(() -> BusinessException.notFound(RoomServiceConstants.ENTITY_ROOM, shortCode));
        return roomMapper.toResponse(room);
    }

    @Transactional(readOnly = true)
    public Page<RoomResponse> getMyRooms(String userId, Pageable pageable) {
        return roomRepository.findByModeratorId(userId, pageable)
            .map(roomMapper::toResponseWithoutDetails);
    }

    @Transactional(readOnly = true)
    public Page<RoomResponse> getJoinedRooms(String userId, Pageable pageable) {
        return roomRepository.findRoomsWhereUserIsParticipant(userId, pageable)
            .map(roomMapper::toResponseWithoutDetails);
    }

    @Transactional(readOnly = true)
    public Page<RoomResponse> getAllRooms(Pageable pageable) {
        return roomRepository.findAllActiveRooms(pageable)
            .map(roomMapper::toResponseWithoutDetails);
    }

    @CacheEvict(value = RoomServiceConstants.CACHE_ROOMS, key = "#id")
    public RoomResponse updateRoom(UUID id, UpdateRoomRequest request, String userId, String userName) {
        RoomEntity room = roomRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound(RoomServiceConstants.ENTITY_ROOM, id));

        if (!room.getModeratorId().equals(userId)) {
            throw BusinessException.forbidden(RoomServiceConstants.ERR_ONLY_MODERATOR_UPDATE_ROOM);
        }

        String previousName = room.getName();
        room.setName(request.name());
        room.setDescription(request.description());

        RoomEntity saved = roomRepository.save(room);

        // Publish event
        RoomUpdatedEvent event = RoomUpdatedEvent.builder()
            .roomId(saved.getId())
            .roomName(saved.getName())
            .description(saved.getDescription())
            .previousName(previousName)
            .build();
        event.initialize(userId, userName);
        eventProducer.publishRoomUpdated(event);

        log.info("Room {} updated by {}", id, userId);
        return roomMapper.toResponse(saved);
    }

    @CacheEvict(value = RoomServiceConstants.CACHE_ROOMS, key = "#id")
    public void deleteRoom(UUID id, String userId, String userName, boolean isAdmin) {
        RoomEntity room = roomRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound(RoomServiceConstants.ENTITY_ROOM, id));

        if (!isAdmin && !room.getModeratorId().equals(userId)) {
            throw BusinessException.forbidden(RoomServiceConstants.ERR_ONLY_MODERATOR_DELETE_ROOM);
        }

        roomRepository.delete(room);

        // Publish event
        RoomDeletedEvent event = RoomDeletedEvent.builder()
            .roomId(room.getId())
            .roomName(room.getName())
            .reason(isAdmin ? "Deleted by admin" : "Deleted by moderator")
            .build();
        event.initialize(userId, userName);
        eventProducer.publishRoomDeleted(event);

        log.info("Room {} deleted by {}", id, userId);
    }

    /**
     * Allows a user to join a room using its short code.
     *
     * @param shortCode the 6-character room code
     * @param userId    the joining user's ID
     * @param userName  the joining user's display name
     * @param userEmail the joining user's email
     * @return the room details
     * @throws BusinessException if room not found or inactive
     */
    public RoomResponse joinRoom(String shortCode, String userId, String userName, String userEmail) {
        RoomEntity room = roomRepository.findByShortCode(shortCode.toUpperCase())
            .orElseThrow(() -> BusinessException.notFound(RoomServiceConstants.ENTITY_ROOM, shortCode));

        if (!room.isActive()) {
            throw BusinessException.conflict(RoomServiceConstants.ERR_ROOM_NOT_ACTIVE);
        }

        // Check if already a participant
        if (!participantRepository.existsByRoomIdAndUserId(room.getId(), userId)) {
            RoomParticipantEntity participant = RoomParticipantEntity.builder()
                .room(room)
                .userId(userId)
                .userName(userName)
                .userEmail(userEmail)
                .role(ParticipantRole.VOTER)
                .online(true)
                .joinedAt(Instant.now())
                .build();
            participantRepository.save(participant);
            log.info("User {} joined room {}", userId, room.getId());
        }

        return roomMapper.toResponse(room);
    }

    private String generateUniqueShortCode() {
        String code;
        int attempts = 0;
        do {
            code = generateShortCode();
            attempts++;
            if (attempts > 100) {
                throw new IllegalStateException("Failed to generate unique short code");
            }
        } while (roomRepository.existsByShortCode(code));
        return code;
    }

    private String generateShortCode() {
        StringBuilder sb = new StringBuilder(RoomServiceConstants.SHORT_CODE_LENGTH);
        for (int i = 0; i < RoomServiceConstants.SHORT_CODE_LENGTH; i++) {
            sb.append(RoomServiceConstants.SHORT_CODE_CHARS.charAt(
                random.nextInt(RoomServiceConstants.SHORT_CODE_CHARS.length())));
        }
        return sb.toString();
    }
}

