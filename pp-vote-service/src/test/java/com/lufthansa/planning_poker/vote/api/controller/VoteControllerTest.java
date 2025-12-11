package com.lufthansa.planning_poker.vote.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lufthansa.planning_poker.vote.application.dto.request.CastVoteRequest;
import com.lufthansa.planning_poker.vote.application.dto.response.VoteResponse;
import com.lufthansa.planning_poker.vote.application.dto.response.VoteResultsResponse;
import com.lufthansa.planning_poker.vote.application.service.VoteService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VoteController.class)
@DisplayName("VoteController Tests")
class VoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VoteService voteService;

    @MockitoBean
    @SuppressWarnings("unused")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private UUID storyId;
    private UUID roomId;
    private UUID voteId;
    private String userId;
    private String userName;
    private VoteResponse voteResponse;
    private VoteResultsResponse voteResultsResponse;

    @BeforeEach
    void setUp() {
        storyId = UUID.randomUUID();
        roomId = UUID.randomUUID();
        voteId = UUID.randomUUID();
        userId = "test-user-id";
        userName = "testuser";

        voteResponse = new VoteResponse(
            voteId, storyId, roomId, userId, userName,
            "8", Instant.now(), null
        );

        voteResultsResponse = new VoteResultsResponse(
            storyId, roomId, 3, new BigDecimal("7.67"),
            null, false,
            List.of(
                new VoteResultsResponse.VoteDetail("user1", "User 1", "5"),
                new VoteResultsResponse.VoteDetail("user2", "User 2", "8"),
                new VoteResultsResponse.VoteDetail("user3", "User 3", "10")
            )
        );
    }

    @Nested
    @DisplayName("POST /api/v1/votes")
    class CastVoteEndpoint {

        @Test
        @DisplayName("Should cast vote successfully")
        void shouldCastVoteSuccessfully() throws Exception {
            // Given
            CastVoteRequest request = new CastVoteRequest(storyId, roomId, "8");

            when(voteService.castVote(any(), anyString(), anyString()))
                .thenReturn(voteResponse);

            // When/Then
            mockMvc.perform(post("/api/v1/votes")
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(voteId.toString()))
                .andExpect(jsonPath("$.value").value("8"))
                .andExpect(jsonPath("$.storyId").value(storyId.toString()));

            verify(voteService).castVote(any(), eq(userId), eq(userName));
        }

        @Test
        @DisplayName("Should return 400 when storyId is null")
        void shouldReturn400WhenStoryIdIsNull() throws Exception {
            // Given
            String requestJson = """
                {
                    "storyId": null,
                    "roomId": "%s",
                    "value": "8"
                }
                """.formatted(roomId);

            // When/Then
            mockMvc.perform(post("/api/v1/votes")
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when value is blank")
        void shouldReturn400WhenValueIsBlank() throws Exception {
            // Given
            CastVoteRequest request = new CastVoteRequest(storyId, roomId, "");

            // When/Then
            mockMvc.perform(post("/api/v1/votes")
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 when not authenticated")
        void shouldReturn403WhenNotAuthenticated() throws Exception {
            // Given
            CastVoteRequest request = new CastVoteRequest(storyId, roomId, "8");

            // When/Then - Spring Security returns 403 for unauthenticated POST requests
            mockMvc.perform(post("/api/v1/votes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should cast vote with non-numeric value")
        void shouldCastVoteWithNonNumericValue() throws Exception {
            // Given
            CastVoteRequest request = new CastVoteRequest(storyId, roomId, "?");
            VoteResponse questionMarkVote = new VoteResponse(
                voteId, storyId, roomId, userId, userName,
                "?", Instant.now(), null
            );

            when(voteService.castVote(any(), anyString(), anyString()))
                .thenReturn(questionMarkVote);

            // When/Then
            mockMvc.perform(post("/api/v1/votes")
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value").value("?"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/votes/stories/{storyId}/my-vote")
    class GetMyVoteEndpoint {

        @Test
        @DisplayName("Should return user's vote when found")
        void shouldReturnMyVoteWhenFound() throws Exception {
            // Given
            when(voteService.getMyVote(storyId, userId)).thenReturn(voteResponse);

            // When/Then
            mockMvc.perform(get("/api/v1/votes/stories/{storyId}/my-vote", storyId)
                    .with(jwt().jwt(jwt -> jwt.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(voteId.toString()))
                .andExpect(jsonPath("$.value").value("8"));
        }

        @Test
        @DisplayName("Should return null when vote not found")
        void shouldReturnNullWhenVoteNotFound() throws Exception {
            // Given
            when(voteService.getMyVote(storyId, userId)).thenReturn(null);

            // When/Then
            mockMvc.perform(get("/api/v1/votes/stories/{storyId}/my-vote", storyId)
                    .with(jwt().jwt(jwt -> jwt.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // When/Then - Spring Security OAuth2 returns 401 for unauthenticated requests
            mockMvc.perform(get("/api/v1/votes/stories/{storyId}/my-vote", storyId))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/votes/stories/{storyId}/count")
    class GetVoteCountEndpoint {

        @Test
        @DisplayName("Should return vote count")
        void shouldReturnVoteCount() throws Exception {
            // Given
            when(voteService.getVoteCount(storyId)).thenReturn(5);

            // When/Then
            mockMvc.perform(get("/api/v1/votes/stories/{storyId}/count", storyId)
                    .with(jwt().jwt(jwt -> jwt.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
        }

        @Test
        @DisplayName("Should return zero when no votes")
        void shouldReturnZeroWhenNoVotes() throws Exception {
            // Given
            when(voteService.getVoteCount(storyId)).thenReturn(0);

            // When/Then
            mockMvc.perform(get("/api/v1/votes/stories/{storyId}/count", storyId)
                    .with(jwt().jwt(jwt -> jwt.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/voting/stories/{storyId}/reveal")
    class RevealVotesEndpoint {

        @Test
        @DisplayName("Should reveal votes successfully")
        void shouldRevealVotesSuccessfully() throws Exception {
            // Given
            when(voteService.revealVotes(eq(storyId), eq(roomId), anyString(), anyString()))
                .thenReturn(voteResultsResponse);

            // When/Then
            mockMvc.perform(post("/api/v1/voting/stories/{storyId}/reveal", storyId)
                    .param("roomId", roomId.toString())
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotes").value(3))
                .andExpect(jsonPath("$.averageScore").value(7.67))
                .andExpect(jsonPath("$.votes").isArray())
                .andExpect(jsonPath("$.votes.length()").value(3));
        }

        @Test
        @DisplayName("Should return 400 when roomId is missing")
        void shouldReturn400WhenRoomIdMissing() throws Exception {
            // When/Then
            mockMvc.perform(post("/api/v1/voting/stories/{storyId}/reveal", storyId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName))))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/voting/stories/{storyId}/finish")
    class FinishVotingEndpoint {

        @Test
        @DisplayName("Should finish voting successfully")
        void shouldFinishVotingSuccessfully() throws Exception {
            // Given
            when(voteService.finishVoting(eq(storyId), eq(roomId), eq("8"), anyString(), anyString(), anyString()))
                .thenReturn(voteResultsResponse);

            // When/Then
            mockMvc.perform(post("/api/v1/voting/stories/{storyId}/finish", storyId)
                    .param("roomId", roomId.toString())
                    .param("finalEstimate", "8")
                    .param("storyTitle", "Test Story")
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotes").value(3));
        }

        @Test
        @DisplayName("Should finish voting without story title")
        void shouldFinishVotingWithoutStoryTitle() throws Exception {
            // Given
            when(voteService.finishVoting(eq(storyId), eq(roomId), eq("8"), eq("Story"), anyString(), anyString()))
                .thenReturn(voteResultsResponse);

            // When/Then
            mockMvc.perform(post("/api/v1/voting/stories/{storyId}/finish", storyId)
                    .param("roomId", roomId.toString())
                    .param("finalEstimate", "8")
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName))))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 400 when finalEstimate is missing")
        void shouldReturn400WhenFinalEstimateMissing() throws Exception {
            // When/Then
            mockMvc.perform(post("/api/v1/voting/stories/{storyId}/finish", storyId)
                    .param("roomId", roomId.toString())
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName))))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/voting/stories/{storyId}/reset")
    class ResetVotesEndpoint {

        @Test
        @DisplayName("Should reset votes successfully")
        void shouldResetVotesSuccessfully() throws Exception {
            // Given
            doNothing().when(voteService).resetVotes(storyId, roomId);

            // When/Then
            mockMvc.perform(post("/api/v1/voting/stories/{storyId}/reset", storyId)
                    .param("roomId", roomId.toString())
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName))))
                .andExpect(status().isNoContent());

            verify(voteService).resetVotes(storyId, roomId);
        }

        @Test
        @DisplayName("Should return 400 when roomId is missing")
        void shouldReturn400WhenRoomIdMissing() throws Exception {
            // When/Then
            mockMvc.perform(post("/api/v1/voting/stories/{storyId}/reset", storyId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName))))
                .andExpect(status().isBadRequest());
        }
    }
}

