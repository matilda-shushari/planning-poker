package com.lufthansa.planning_poker.room.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lufthansa.planning_poker.room.application.dto.request.SendInviteRequest;
import com.lufthansa.planning_poker.room.application.dto.response.InviteResponse;
import com.lufthansa.planning_poker.room.application.service.InvitationService;
import com.lufthansa.planning_poker.room.api.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InvitationController.class)
@DisplayName("InvitationController Tests")
class InvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InvitationService invitationService;

    @MockitoBean
    @SuppressWarnings("unused")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private UUID roomId;
    private UUID invitationId;
    private String moderatorId;
    private String moderatorName;
    private InviteResponse inviteResponse;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        invitationId = UUID.randomUUID();
        moderatorId = "moderator-user-id";
        moderatorName = "moderator";

        inviteResponse = new InviteResponse(
            invitationId,
            roomId,
            "Sprint 42 Planning",
            "invited@example.com",
            "PENDING",
            "http://localhost:4200/join/invite/abc123token",
            Instant.now().plus(48, ChronoUnit.HOURS),
            Instant.now()
        );
    }

    @Nested
    @DisplayName("POST /api/v1/rooms/{roomId}/invitations")
    class SendInvitationEndpoint {

        @Test
        @DisplayName("Should send invitation successfully")
        void shouldSendInvitationSuccessfully() throws Exception {
            // Given
            SendInviteRequest request = new SendInviteRequest("invited@example.com");

            when(invitationService.sendInvitation(eq(roomId), any(), anyString(), anyString()))
                .thenReturn(inviteResponse);

            // When/Then
            mockMvc.perform(post("/api/v1/rooms/{roomId}/invitations", roomId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(moderatorId)
                        .claim("preferred_username", moderatorName)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(invitationId.toString()))
                .andExpect(jsonPath("$.email").value("invited@example.com"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.inviteLink").exists());

            verify(invitationService).sendInvitation(eq(roomId), any(), eq(moderatorId), eq(moderatorName));
        }

        @Test
        @DisplayName("Should return 400 when email is invalid")
        void shouldReturn400WhenEmailIsInvalid() throws Exception {
            // Given
            SendInviteRequest request = new SendInviteRequest("invalid-email");

            // When/Then
            mockMvc.perform(post("/api/v1/rooms/{roomId}/invitations", roomId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(moderatorId)
                        .claim("preferred_username", moderatorName)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when email is blank")
        void shouldReturn400WhenEmailIsBlank() throws Exception {
            // Given
            SendInviteRequest request = new SendInviteRequest("");

            // When/Then
            mockMvc.perform(post("/api/v1/rooms/{roomId}/invitations", roomId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(moderatorId)
                        .claim("preferred_username", moderatorName)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 when non-moderator sends invitation")
        void shouldReturn403WhenNonModeratorSendsInvitation() throws Exception {
            // Given
            SendInviteRequest request = new SendInviteRequest("invited@example.com");

            when(invitationService.sendInvitation(eq(roomId), any(), anyString(), anyString()))
                .thenThrow(BusinessException.forbidden("Only the moderator can send invitations"));

            // When/Then
            mockMvc.perform(post("/api/v1/rooms/{roomId}/invitations", roomId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject("other-user")
                        .claim("preferred_username", "other")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 409 when invitation already pending")
        void shouldReturn409WhenInvitationAlreadyPending() throws Exception {
            // Given
            SendInviteRequest request = new SendInviteRequest("invited@example.com");

            when(invitationService.sendInvitation(eq(roomId), any(), anyString(), anyString()))
                .thenThrow(BusinessException.conflict("An invitation is already pending for this email"));

            // When/Then
            mockMvc.perform(post("/api/v1/rooms/{roomId}/invitations", roomId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(moderatorId)
                        .claim("preferred_username", moderatorName)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 404 when room not found")
        void shouldReturn404WhenRoomNotFound() throws Exception {
            // Given
            SendInviteRequest request = new SendInviteRequest("invited@example.com");
            UUID nonExistentRoomId = UUID.randomUUID();

            when(invitationService.sendInvitation(eq(nonExistentRoomId), any(), anyString(), anyString()))
                .thenThrow(BusinessException.notFound("Room", nonExistentRoomId));

            // When/Then
            mockMvc.perform(post("/api/v1/rooms/{roomId}/invitations", nonExistentRoomId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(moderatorId)
                        .claim("preferred_username", moderatorName)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when not authenticated")
        void shouldReturn403WhenNotAuthenticated() throws Exception {
            // Given
            SendInviteRequest request = new SendInviteRequest("invited@example.com");

            // When/Then
            mockMvc.perform(post("/api/v1/rooms/{roomId}/invitations", roomId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/rooms/{roomId}/invitations")
    class GetInvitationsEndpoint {

        @Test
        @DisplayName("Should return all invitations for room")
        void shouldReturnAllInvitationsForRoom() throws Exception {
            // Given
            InviteResponse acceptedInvite = new InviteResponse(
                UUID.randomUUID(), roomId, "Sprint 42 Planning",
                "accepted@example.com", "ACCEPTED",
                null, Instant.now().plus(48, ChronoUnit.HOURS), Instant.now()
            );
            List<InviteResponse> invitations = List.of(inviteResponse, acceptedInvite);

            when(invitationService.getInvitationsByRoom(roomId, moderatorId))
                .thenReturn(invitations);

            // When/Then
            mockMvc.perform(get("/api/v1/rooms/{roomId}/invitations", roomId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(moderatorId)
                        .claim("preferred_username", moderatorName))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].status").value("ACCEPTED"));
        }

        @Test
        @DisplayName("Should return empty list when no invitations")
        void shouldReturnEmptyListWhenNoInvitations() throws Exception {
            // Given
            when(invitationService.getInvitationsByRoom(roomId, moderatorId))
                .thenReturn(List.of());

            // When/Then
            mockMvc.perform(get("/api/v1/rooms/{roomId}/invitations", roomId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(moderatorId)
                        .claim("preferred_username", moderatorName))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Should return 403 when non-moderator views invitations")
        void shouldReturn403WhenNonModeratorViewsInvitations() throws Exception {
            // Given
            when(invitationService.getInvitationsByRoom(eq(roomId), anyString()))
                .thenThrow(BusinessException.forbidden("Only the moderator can view invitations"));

            // When/Then
            mockMvc.perform(get("/api/v1/rooms/{roomId}/invitations", roomId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject("other-user")
                        .claim("preferred_username", "other"))))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/invitations/{token}/accept")
    class AcceptInvitationEndpoint {

        @Test
        @DisplayName("Should accept invitation successfully")
        void shouldAcceptInvitationSuccessfully() throws Exception {
            // Given
            String token = "valid-invitation-token";
            InviteResponse acceptedResponse = new InviteResponse(
                invitationId, roomId, "Sprint 42 Planning",
                "invited@example.com", "ACCEPTED",
                null, Instant.now().plus(48, ChronoUnit.HOURS), Instant.now()
            );

            when(invitationService.acceptInvitation(eq(token), anyString(), anyString(), anyString()))
                .thenReturn(acceptedResponse);

            // When/Then
            mockMvc.perform(post("/api/v1/invitations/{token}/accept", token)
                    .with(jwt().jwt(jwt -> jwt
                        .subject("new-user")
                        .claim("preferred_username", "newuser")
                        .claim("email", "invited@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.roomId").value(roomId.toString()));
        }

        @Test
        @DisplayName("Should return 404 when invitation not found")
        void shouldReturn404WhenInvitationNotFound() throws Exception {
            // Given
            String invalidToken = "invalid-token";

            when(invitationService.acceptInvitation(eq(invalidToken), anyString(), anyString(), anyString()))
                .thenThrow(BusinessException.notFound("Invitation", invalidToken));

            // When/Then
            mockMvc.perform(post("/api/v1/invitations/{token}/accept", invalidToken)
                    .with(jwt().jwt(jwt -> jwt
                        .subject("new-user")
                        .claim("preferred_username", "newuser")
                        .claim("email", "user@example.com"))))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 409 when invitation already accepted")
        void shouldReturn409WhenInvitationAlreadyAccepted() throws Exception {
            // Given
            String token = "already-accepted-token";

            when(invitationService.acceptInvitation(eq(token), anyString(), anyString(), anyString()))
                .thenThrow(BusinessException.conflict("This invitation has already been accepted"));

            // When/Then
            mockMvc.perform(post("/api/v1/invitations/{token}/accept", token)
                    .with(jwt().jwt(jwt -> jwt
                        .subject("new-user")
                        .claim("preferred_username", "newuser")
                        .claim("email", "user@example.com"))))
                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 409 when invitation expired")
        void shouldReturn409WhenInvitationExpired() throws Exception {
            // Given
            String expiredToken = "expired-token";

            when(invitationService.acceptInvitation(eq(expiredToken), anyString(), anyString(), anyString()))
                .thenThrow(BusinessException.conflict("This invitation has expired"));

            // When/Then
            mockMvc.perform(post("/api/v1/invitations/{token}/accept", expiredToken)
                    .with(jwt().jwt(jwt -> jwt
                        .subject("new-user")
                        .claim("preferred_username", "newuser")
                        .claim("email", "user@example.com"))))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/invitations/{id}")
    class CancelInvitationEndpoint {

        @Test
        @DisplayName("Should cancel invitation successfully")
        void shouldCancelInvitationSuccessfully() throws Exception {
            // Given
            doNothing().when(invitationService).cancelInvitation(invitationId, moderatorId);

            // When/Then
            mockMvc.perform(delete("/api/v1/invitations/{id}", invitationId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(moderatorId)
                        .claim("preferred_username", moderatorName))))
                .andExpect(status().isNoContent());

            verify(invitationService).cancelInvitation(invitationId, moderatorId);
        }

        @Test
        @DisplayName("Should return 404 when invitation not found")
        void shouldReturn404WhenInvitationNotFound() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();

            doThrow(BusinessException.notFound("Invitation", nonExistentId))
                .when(invitationService).cancelInvitation(nonExistentId, moderatorId);

            // When/Then
            mockMvc.perform(delete("/api/v1/invitations/{id}", nonExistentId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(moderatorId)
                        .claim("preferred_username", moderatorName))))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when non-moderator cancels invitation")
        void shouldReturn403WhenNonModeratorCancelsInvitation() throws Exception {
            // Given
            doThrow(BusinessException.forbidden("Only the moderator can cancel invitations"))
                .when(invitationService).cancelInvitation(eq(invitationId), anyString());

            // When/Then
            mockMvc.perform(delete("/api/v1/invitations/{id}", invitationId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject("other-user")
                        .claim("preferred_username", "other"))))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 409 when cancelling non-pending invitation")
        void shouldReturn409WhenCancellingNonPendingInvitation() throws Exception {
            // Given
            doThrow(BusinessException.conflict("Can only cancel pending invitations"))
                .when(invitationService).cancelInvitation(invitationId, moderatorId);

            // When/Then
            mockMvc.perform(delete("/api/v1/invitations/{id}", invitationId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(moderatorId)
                        .claim("preferred_username", moderatorName))))
                .andExpect(status().isConflict());
        }
    }
}

