package com.lufthansa.planning_poker.room.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lufthansa.planning_poker.room.application.dto.request.CreateRoomRequest;
import com.lufthansa.planning_poker.room.application.dto.request.UpdateRoomRequest;
import com.lufthansa.planning_poker.room.application.dto.response.RoomResponse;
import com.lufthansa.planning_poker.room.application.service.RoomService;
import com.lufthansa.planning_poker.room.domain.model.DeckType;
import com.lufthansa.planning_poker.room.api.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
@DisplayName("RoomController Tests")
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    @SuppressWarnings("unused")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private UUID roomId;
    private String userId;
    private String userName;
    private RoomResponse roomResponse;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        userId = "test-user-id";
        userName = "testuser";

        roomResponse = new RoomResponse(
            roomId, "Sprint 42 Planning", "Test description",
            DeckType.FIBONACCI, DeckType.FIBONACCI.getDefaultValues(),
            userId, userName, "ABC123", "/join/ABC123",
            true, Instant.now(), null, 1, 0,
            Collections.emptyList(), Collections.emptyList()
        );
    }

    @Nested
    @DisplayName("POST /api/v1/rooms")
    class CreateRoomEndpoint {

        @Test
        @DisplayName("Should create room successfully with valid request")
        void shouldCreateRoomSuccessfully() throws Exception {
            // Given
            CreateRoomRequest request = new CreateRoomRequest(
                "Sprint 42 Planning",
                "Test description",
                DeckType.FIBONACCI,
                null
            );

            when(roomService.createRoom(any(), anyString(), anyString()))
                .thenReturn(roomResponse);

            // When/Then
            mockMvc.perform(post("/api/v1/rooms")
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(roomId.toString()))
                .andExpect(jsonPath("$.name").value("Sprint 42 Planning"));
        }

        @Test
        @DisplayName("Should return 400 when name is blank")
        void shouldReturn400WhenNameIsBlank() throws Exception {
            // Given
            CreateRoomRequest request = new CreateRoomRequest(
                "",
                "Test description",
                DeckType.FIBONACCI,
                null
            );

            // When/Then
            mockMvc.perform(post("/api/v1/rooms")
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when deck type is null")
        void shouldReturn400WhenDeckTypeIsNull() throws Exception {
            // Given
            String requestJson = """
                {
                    "name": "Test Room",
                    "description": "Test",
                    "deckType": null
                }
                """;

            // When/Then
            mockMvc.perform(post("/api/v1/rooms")
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 when not authenticated")
        void shouldReturn403WhenNotAuthenticated() throws Exception {
            // Given
            CreateRoomRequest request = new CreateRoomRequest(
                "Test Room",
                null,
                DeckType.FIBONACCI,
                null
            );

            // When/Then
            // Note: Spring Security OAuth2 Resource Server returns 403 for unauthenticated requests
            // when using @WebMvcTest with default security configuration
            mockMvc.perform(post("/api/v1/rooms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/rooms/{id}")
    class GetRoomByIdEndpoint {

        @Test
        @DisplayName("Should return room when found")
        void shouldReturnRoomWhenFound() throws Exception {
            // Given
            when(roomService.getRoomById(roomId)).thenReturn(roomResponse);

            // When/Then
            mockMvc.perform(get("/api/v1/rooms/{id}", roomId)
                    .with(jwt().jwt(jwt -> jwt.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roomId.toString()))
                .andExpect(jsonPath("$.name").value("Sprint 42 Planning"));
        }

        @Test
        @DisplayName("Should return 404 when room not found")
        void shouldReturn404WhenRoomNotFound() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(roomService.getRoomById(nonExistentId))
                .thenThrow(BusinessException.notFound("Room", nonExistentId));

            // When/Then
            mockMvc.perform(get("/api/v1/rooms/{id}", nonExistentId)
                    .with(jwt().jwt(jwt -> jwt.subject(userId))))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/rooms/code/{shortCode}")
    class GetRoomByShortCodeEndpoint {

        @Test
        @DisplayName("Should return room when found by short code")
        void shouldReturnRoomWhenFoundByShortCode() throws Exception {
            // Given
            String shortCode = "ABC123";
            when(roomService.getRoomByShortCode(shortCode)).thenReturn(roomResponse);

            // When/Then
            mockMvc.perform(get("/api/v1/rooms/code/{shortCode}", shortCode)
                    .with(jwt().jwt(jwt -> jwt.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("ABC123"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/rooms/my-rooms")
    class GetMyRoomsEndpoint {

        @Test
        @DisplayName("Should return paginated rooms for moderator")
        void shouldReturnPaginatedRooms() throws Exception {
            // Given
            List<RoomResponse> rooms = List.of(roomResponse);
            Page<RoomResponse> roomPage = new PageImpl<>(rooms);
            
            when(roomService.getMyRooms(eq(userId), any(Pageable.class))).thenReturn(roomPage);

            // When/Then
            mockMvc.perform(get("/api/v1/rooms/my-rooms")
                    .with(jwt().jwt(jwt -> jwt.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(roomId.toString()));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/rooms/{id}")
    class UpdateRoomEndpoint {

        @Test
        @DisplayName("Should update room successfully")
        void shouldUpdateRoomSuccessfully() throws Exception {
            // Given
            UpdateRoomRequest request = new UpdateRoomRequest("Updated Name", "Updated Description");
            
            when(roomService.updateRoom(eq(roomId), any(), anyString(), anyString()))
                .thenReturn(roomResponse);

            // When/Then
            mockMvc.perform(put("/api/v1/rooms/{id}", roomId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 403 when non-moderator tries to update")
        void shouldReturn403WhenNonModerator() throws Exception {
            // Given
            UpdateRoomRequest request = new UpdateRoomRequest("Updated", "Description");
            
            when(roomService.updateRoom(eq(roomId), any(), anyString(), anyString()))
                .thenThrow(BusinessException.forbidden("Only the moderator can update this room"));

            // When/Then
            mockMvc.perform(put("/api/v1/rooms/{id}", roomId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject("other-user")
                        .claim("preferred_username", "other")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/rooms/{id}")
    class DeleteRoomEndpoint {

        @Test
        @DisplayName("Should delete room successfully")
        void shouldDeleteRoomSuccessfully() throws Exception {
            // Given
            doNothing().when(roomService).deleteRoom(eq(roomId), anyString(), anyString(), anyBoolean());

            // When/Then
            mockMvc.perform(delete("/api/v1/rooms/{id}", roomId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName))))
                .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/rooms/join/{shortCode}")
    class JoinRoomEndpoint {

        @Test
        @DisplayName("Should join room successfully")
        void shouldJoinRoomSuccessfully() throws Exception {
            // Given
            String shortCode = "ABC123";
            when(roomService.joinRoom(eq(shortCode), anyString(), anyString(), anyString()))
                .thenReturn(roomResponse);

            // When/Then
            mockMvc.perform(post("/api/v1/rooms/join/{shortCode}", shortCode)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName)
                        .claim("email", "test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("ABC123"));
        }
    }
}
