package com.lufthansa.planning_poker.room.application.service;

import com.lufthansa.planning_poker.room.api.exception.BusinessException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomService Tests")
class RoomServiceTest {

    @Mock
    private JpaRoomRepository roomRepository;

    @Mock
    private JpaParticipantRepository participantRepository;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private RoomEventProducer eventProducer;

    @InjectMocks
    private RoomService roomService;

    private UUID roomId;
    private String moderatorId;
    private String moderatorName;
    private RoomEntity roomEntity;
    private RoomResponse roomResponse;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        moderatorId = "user-123";
        moderatorName = "testuser";

        roomEntity = RoomEntity.builder()
            .id(roomId)
            .name("Sprint 42 Planning")
            .description("Test description")
            .deckType(DeckType.FIBONACCI)
            .deckValues(DeckType.FIBONACCI.getDefaultValues())
            .moderatorId(moderatorId)
            .moderatorName(moderatorName)
            .shortCode("ABC123")
            .active(true)
            .createdAt(Instant.now())
            .build();

        roomResponse = new RoomResponse(
            roomId, "Sprint 42 Planning", "Test description",
            DeckType.FIBONACCI, DeckType.FIBONACCI.getDefaultValues(),
            moderatorId, moderatorName, "ABC123", "/join/ABC123",
            true, Instant.now(), null, 1, 0,
            Collections.emptyList(), Collections.emptyList()
        );
    }

    @Nested
    @DisplayName("createRoom Tests")
    class CreateRoomTests {

        @Test
        @DisplayName("Should create room successfully with FIBONACCI deck")
        void shouldCreateRoomSuccessfully() {
            // Given
            CreateRoomRequest request = new CreateRoomRequest(
                "Sprint 42 Planning",
                "Test description",
                DeckType.FIBONACCI,
                null
            );

            when(roomRepository.existsByShortCode(anyString())).thenReturn(false);
            when(roomRepository.save(any(RoomEntity.class))).thenReturn(roomEntity);
            when(participantRepository.save(any(RoomParticipantEntity.class)))
                .thenReturn(RoomParticipantEntity.builder().build());
            when(roomMapper.toResponse(any(RoomEntity.class))).thenReturn(roomResponse);

            // When
            RoomResponse result = roomService.createRoom(request, moderatorId, moderatorName);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("Sprint 42 Planning");
            verify(roomRepository).save(any(RoomEntity.class));
            verify(participantRepository).save(any(RoomParticipantEntity.class));
            verify(eventProducer).publishRoomCreated(any());
        }

        @Test
        @DisplayName("Should create room with CUSTOM deck values")
        void shouldCreateRoomWithCustomDeck() {
            // Given
            List<String> customValues = List.of("S", "M", "L", "XL");
            CreateRoomRequest request = new CreateRoomRequest(
                "Custom Room",
                "Custom deck test",
                DeckType.CUSTOM,
                customValues
            );

            RoomEntity customRoom = RoomEntity.builder()
                .id(roomId)
                .name("Custom Room")
                .deckType(DeckType.CUSTOM)
                .deckValues(customValues)
                .moderatorId(moderatorId)
                .moderatorName(moderatorName)
                .shortCode("XYZ789")
                .active(true)
                .build();

            when(roomRepository.existsByShortCode(anyString())).thenReturn(false);
            when(roomRepository.save(any(RoomEntity.class))).thenReturn(customRoom);
            when(participantRepository.save(any(RoomParticipantEntity.class)))
                .thenReturn(RoomParticipantEntity.builder().build());
            when(roomMapper.toResponse(any(RoomEntity.class))).thenReturn(roomResponse);

            // When
            RoomResponse result = roomService.createRoom(request, moderatorId, moderatorName);

            // Then
            assertThat(result).isNotNull();
            ArgumentCaptor<RoomEntity> roomCaptor = ArgumentCaptor.forClass(RoomEntity.class);
            verify(roomRepository).save(roomCaptor.capture());
            assertThat(roomCaptor.getValue().getDeckValues()).isEqualTo(customValues);
        }

        @Test
        @DisplayName("Should create room with SCRUM deck")
        void shouldCreateRoomWithScrumDeck() {
            // Given
            CreateRoomRequest request = new CreateRoomRequest(
                "Scrum Room",
                null,
                DeckType.SCRUM,
                null
            );

            when(roomRepository.existsByShortCode(anyString())).thenReturn(false);
            when(roomRepository.save(any(RoomEntity.class))).thenReturn(roomEntity);
            when(participantRepository.save(any(RoomParticipantEntity.class)))
                .thenReturn(RoomParticipantEntity.builder().build());
            when(roomMapper.toResponse(any(RoomEntity.class))).thenReturn(roomResponse);

            // When
            roomService.createRoom(request, moderatorId, moderatorName);

            // Then
            ArgumentCaptor<RoomEntity> roomCaptor = ArgumentCaptor.forClass(RoomEntity.class);
            verify(roomRepository).save(roomCaptor.capture());
            assertThat(roomCaptor.getValue().getDeckValues()).isEqualTo(DeckType.SCRUM.getDefaultValues());
        }

        @Test
        @DisplayName("Should add creator as MODERATOR participant")
        void shouldAddCreatorAsModerator() {
            // Given
            CreateRoomRequest request = new CreateRoomRequest("Test", null, DeckType.FIBONACCI, null);

            when(roomRepository.existsByShortCode(anyString())).thenReturn(false);
            when(roomRepository.save(any(RoomEntity.class))).thenReturn(roomEntity);
            when(participantRepository.save(any(RoomParticipantEntity.class)))
                .thenReturn(RoomParticipantEntity.builder().build());
            when(roomMapper.toResponse(any(RoomEntity.class))).thenReturn(roomResponse);

            // When
            roomService.createRoom(request, moderatorId, moderatorName);

            // Then
            ArgumentCaptor<RoomParticipantEntity> participantCaptor = 
                ArgumentCaptor.forClass(RoomParticipantEntity.class);
            verify(participantRepository).save(participantCaptor.capture());
            
            RoomParticipantEntity savedParticipant = participantCaptor.getValue();
            assertThat(savedParticipant.getRole()).isEqualTo(ParticipantRole.MODERATOR);
            assertThat(savedParticipant.getUserId()).isEqualTo(moderatorId);
        }
    }

    @Nested
    @DisplayName("getRoomById Tests")
    class GetRoomByIdTests {

        @Test
        @DisplayName("Should return room when found")
        void shouldReturnRoomWhenFound() {
            // Given
            when(roomRepository.findByIdWithDetails(roomId)).thenReturn(Optional.of(roomEntity));
            when(roomMapper.toResponse(roomEntity)).thenReturn(roomResponse);

            // When
            RoomResponse result = roomService.getRoomById(roomId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(roomId);
        }

        @Test
        @DisplayName("Should throw BusinessException when room not found")
        void shouldThrowWhenRoomNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(roomRepository.findByIdWithDetails(nonExistentId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> roomService.getRoomById(nonExistentId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("getRoomByShortCode Tests")
    class GetRoomByShortCodeTests {

        @Test
        @DisplayName("Should return room when found by short code")
        void shouldReturnRoomWhenFoundByShortCode() {
            // Given
            String shortCode = "ABC123";
            when(roomRepository.findByShortCodeWithDetails(shortCode)).thenReturn(Optional.of(roomEntity));
            when(roomMapper.toResponse(roomEntity)).thenReturn(roomResponse);

            // When
            RoomResponse result = roomService.getRoomByShortCode(shortCode);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.shortCode()).isEqualTo(shortCode);
        }

        @Test
        @DisplayName("Should convert short code to uppercase")
        void shouldConvertShortCodeToUppercase() {
            // Given
            String shortCode = "abc123";
            when(roomRepository.findByShortCodeWithDetails("ABC123")).thenReturn(Optional.of(roomEntity));
            when(roomMapper.toResponse(roomEntity)).thenReturn(roomResponse);

            // When
            roomService.getRoomByShortCode(shortCode);

            // Then
            verify(roomRepository).findByShortCodeWithDetails("ABC123");
        }

        @Test
        @DisplayName("Should throw BusinessException when room not found by short code")
        void shouldThrowWhenRoomNotFoundByShortCode() {
            // Given
            String shortCode = "INVALID";
            when(roomRepository.findByShortCodeWithDetails(shortCode)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> roomService.getRoomByShortCode(shortCode))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("getMyRooms Tests")
    class GetMyRoomsTests {

        @Test
        @DisplayName("Should return paginated rooms for moderator")
        void shouldReturnPaginatedRoomsForModerator() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<RoomEntity> rooms = List.of(roomEntity);
            Page<RoomEntity> roomPage = new PageImpl<>(rooms, pageable, 1);
            
            when(roomRepository.findByModeratorId(moderatorId, pageable)).thenReturn(roomPage);
            when(roomMapper.toResponseWithoutDetails(roomEntity)).thenReturn(roomResponse);

            // When
            Page<RoomResponse> result = roomService.getMyRooms(moderatorId, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty page when no rooms found")
        void shouldReturnEmptyPageWhenNoRooms() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<RoomEntity> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            
            when(roomRepository.findByModeratorId(moderatorId, pageable)).thenReturn(emptyPage);

            // When
            Page<RoomResponse> result = roomService.getMyRooms(moderatorId, pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateRoom Tests")
    class UpdateRoomTests {

        @Test
        @DisplayName("Should update room successfully")
        void shouldUpdateRoomSuccessfully() {
            // Given
            UpdateRoomRequest request = new UpdateRoomRequest("Updated Name", "Updated Description");
            
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(roomEntity));
            when(roomRepository.save(any(RoomEntity.class))).thenReturn(roomEntity);
            when(roomMapper.toResponse(any(RoomEntity.class))).thenReturn(roomResponse);

            // When
            RoomResponse result = roomService.updateRoom(roomId, request, moderatorId, moderatorName);

            // Then
            assertThat(result).isNotNull();
            verify(roomRepository).save(any(RoomEntity.class));
            verify(eventProducer).publishRoomUpdated(any());
        }

        @Test
        @DisplayName("Should throw BusinessException when non-moderator tries to update")
        void shouldThrowWhenNonModeratorTriesToUpdate() {
            // Given
            UpdateRoomRequest request = new UpdateRoomRequest("Updated", "Description");
            String differentUserId = "other-user";
            
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(roomEntity));

            // When/Then
            assertThatThrownBy(() -> roomService.updateRoom(roomId, request, differentUserId, "other"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("moderator");
        }

        @Test
        @DisplayName("Should throw BusinessException when room not found")
        void shouldThrowWhenRoomNotFoundForUpdate() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            UpdateRoomRequest request = new UpdateRoomRequest("Name", "Description");
            
            when(roomRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> roomService.updateRoom(nonExistentId, request, moderatorId, moderatorName))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("deleteRoom Tests")
    class DeleteRoomTests {

        @Test
        @DisplayName("Should delete room when user is moderator")
        void shouldDeleteRoomWhenModerator() {
            // Given
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(roomEntity));

            // When
            roomService.deleteRoom(roomId, moderatorId, moderatorName, false);

            // Then
            verify(roomRepository).delete(roomEntity);
            verify(eventProducer).publishRoomDeleted(any());
        }

        @Test
        @DisplayName("Should delete room when user is admin")
        void shouldDeleteRoomWhenAdmin() {
            // Given
            String adminId = "admin-user";
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(roomEntity));

            // When
            roomService.deleteRoom(roomId, adminId, "admin", true);

            // Then
            verify(roomRepository).delete(roomEntity);
            verify(eventProducer).publishRoomDeleted(any());
        }

        @Test
        @DisplayName("Should throw BusinessException when non-moderator non-admin tries to delete")
        void shouldThrowWhenUnauthorizedDelete() {
            // Given
            String randomUserId = "random-user";
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(roomEntity));

            // When/Then
            assertThatThrownBy(() -> roomService.deleteRoom(roomId, randomUserId, "random", false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("moderator or admin");
        }
    }

    @Nested
    @DisplayName("joinRoom Tests")
    class JoinRoomTests {

        @Test
        @DisplayName("Should allow user to join room")
        void shouldAllowUserToJoinRoom() {
            // Given
            String newUserId = "new-user";
            String newUserName = "newuser";
            String shortCode = "ABC123";
            
            when(roomRepository.findByShortCode(shortCode)).thenReturn(Optional.of(roomEntity));
            when(participantRepository.existsByRoomIdAndUserId(roomId, newUserId)).thenReturn(false);
            when(participantRepository.save(any(RoomParticipantEntity.class)))
                .thenReturn(RoomParticipantEntity.builder().build());
            when(roomMapper.toResponse(roomEntity)).thenReturn(roomResponse);

            // When
            RoomResponse result = roomService.joinRoom(shortCode, newUserId, newUserName, "email@test.com");

            // Then
            assertThat(result).isNotNull();
            verify(participantRepository).save(any(RoomParticipantEntity.class));
        }

        @Test
        @DisplayName("Should not duplicate participant when already joined")
        void shouldNotDuplicateParticipant() {
            // Given
            String existingUserId = "existing-user";
            String shortCode = "ABC123";
            
            when(roomRepository.findByShortCode(shortCode)).thenReturn(Optional.of(roomEntity));
            when(participantRepository.existsByRoomIdAndUserId(roomId, existingUserId)).thenReturn(true);
            when(roomMapper.toResponse(roomEntity)).thenReturn(roomResponse);

            // When
            roomService.joinRoom(shortCode, existingUserId, "existing", "email@test.com");

            // Then
            verify(participantRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BusinessException when room is inactive")
        void shouldThrowWhenRoomInactive() {
            // Given
            roomEntity.setActive(false);
            String shortCode = "ABC123";
            
            when(roomRepository.findByShortCode(shortCode)).thenReturn(Optional.of(roomEntity));

            // When/Then
            assertThatThrownBy(() -> roomService.joinRoom(shortCode, "user", "name", "email@test.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no longer active");
        }

        @Test
        @DisplayName("Should add participant with VOTER role")
        void shouldAddParticipantAsVoter() {
            // Given
            String newUserId = "new-user";
            String shortCode = "ABC123";
            
            when(roomRepository.findByShortCode(shortCode)).thenReturn(Optional.of(roomEntity));
            when(participantRepository.existsByRoomIdAndUserId(roomId, newUserId)).thenReturn(false);
            when(participantRepository.save(any(RoomParticipantEntity.class)))
                .thenReturn(RoomParticipantEntity.builder().build());
            when(roomMapper.toResponse(roomEntity)).thenReturn(roomResponse);

            // When
            roomService.joinRoom(shortCode, newUserId, "name", "email@test.com");

            // Then
            ArgumentCaptor<RoomParticipantEntity> participantCaptor = 
                ArgumentCaptor.forClass(RoomParticipantEntity.class);
            verify(participantRepository).save(participantCaptor.capture());
            assertThat(participantCaptor.getValue().getRole()).isEqualTo(ParticipantRole.VOTER);
        }
    }

    @Nested
    @DisplayName("getJoinedRooms Tests")
    class GetJoinedRoomsTests {

        @Test
        @DisplayName("Should return rooms where user is participant")
        void shouldReturnJoinedRooms() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<RoomEntity> rooms = List.of(roomEntity);
            Page<RoomEntity> roomPage = new PageImpl<>(rooms, pageable, 1);
            
            when(roomRepository.findRoomsWhereUserIsParticipant(moderatorId, pageable)).thenReturn(roomPage);
            when(roomMapper.toResponseWithoutDetails(roomEntity)).thenReturn(roomResponse);

            // When
            Page<RoomResponse> result = roomService.getJoinedRooms(moderatorId, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }
}

