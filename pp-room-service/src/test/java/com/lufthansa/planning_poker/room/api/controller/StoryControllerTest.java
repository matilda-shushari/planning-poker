package com.lufthansa.planning_poker.room.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lufthansa.planning_poker.room.application.dto.request.CreateStoryRequest;
import com.lufthansa.planning_poker.room.application.dto.request.UpdateStoryRequest;
import com.lufthansa.planning_poker.room.application.dto.response.StoryResponse;
import com.lufthansa.planning_poker.room.application.service.StoryService;
import com.lufthansa.planning_poker.room.domain.model.StoryStatus;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StoryController.class)
@DisplayName("StoryController Tests")
class StoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StoryService storyService;

    @MockitoBean
    @SuppressWarnings("unused")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private UUID roomId;
    private UUID storyId;
    private String userId;
    private String userName;
    private StoryResponse storyResponse;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        storyId = UUID.randomUUID();
        userId = "test-user-id";
        userName = "testuser";

        storyResponse = new StoryResponse(
            storyId, roomId, "User Authentication",
            "Implement OAuth2 login", "https://jira.example.com/PROJ-101",
            StoryStatus.PENDING, null, null, 1, Instant.now(), null, null, null
        );
    }

    @Nested
    @DisplayName("POST /api/v1/rooms/{roomId}/stories")
    class CreateStoryEndpoint {

        @Test
        @DisplayName("Should create story successfully")
        void shouldCreateStorySuccessfully() throws Exception {
            // Given
            CreateStoryRequest request = new CreateStoryRequest(
                "User Authentication",
                "Implement OAuth2 login",
                "https://jira.example.com/PROJ-101"
            );

            when(storyService.createStory(eq(roomId), any(), anyString(), anyString()))
                .thenReturn(storyResponse);

            // When/Then
            mockMvc.perform(post("/api/v1/rooms/{roomId}/stories", roomId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(storyId.toString()))
                .andExpect(jsonPath("$.title").value("User Authentication"));
        }

        @Test
        @DisplayName("Should return 400 when title is blank")
        void shouldReturn400WhenTitleIsBlank() throws Exception {
            // Given
            CreateStoryRequest request = new CreateStoryRequest(
                "",
                "Description",
                null
            );

            // When/Then
            mockMvc.perform(post("/api/v1/rooms/{roomId}/stories", roomId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 when non-moderator creates story")
        void shouldReturn403WhenNonModerator() throws Exception {
            // Given
            CreateStoryRequest request = new CreateStoryRequest("Title", "Description", null);

            when(storyService.createStory(eq(roomId), any(), anyString(), anyString()))
                .thenThrow(BusinessException.forbidden("Only the moderator can create stories"));

            // When/Then
            mockMvc.perform(post("/api/v1/rooms/{roomId}/stories", roomId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject("other-user")
                        .claim("preferred_username", "other")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/rooms/{roomId}/stories")
    class GetStoriesByRoomEndpoint {

        @Test
        @DisplayName("Should return all stories for room")
        void shouldReturnAllStoriesForRoom() throws Exception {
            // Given
            List<StoryResponse> stories = List.of(storyResponse);
            when(storyService.getStoriesByRoomId(roomId)).thenReturn(stories);

            // When/Then
            mockMvc.perform(get("/api/v1/rooms/{roomId}/stories", roomId)
                    .with(jwt().jwt(jwt -> jwt.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(storyId.toString()));
        }

        @Test
        @DisplayName("Should return empty list when no stories")
        void shouldReturnEmptyListWhenNoStories() throws Exception {
            // Given
            when(storyService.getStoriesByRoomId(roomId)).thenReturn(List.of());

            // When/Then
            mockMvc.perform(get("/api/v1/rooms/{roomId}/stories", roomId)
                    .with(jwt().jwt(jwt -> jwt.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/stories/{id}")
    class GetStoryByIdEndpoint {

        @Test
        @DisplayName("Should return story when found")
        void shouldReturnStoryWhenFound() throws Exception {
            // Given
            when(storyService.getStoryById(storyId)).thenReturn(storyResponse);

            // When/Then
            mockMvc.perform(get("/api/v1/stories/{id}", storyId)
                    .with(jwt().jwt(jwt -> jwt.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(storyId.toString()));
        }

        @Test
        @DisplayName("Should return 404 when story not found")
        void shouldReturn404WhenStoryNotFound() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(storyService.getStoryById(nonExistentId))
                .thenThrow(BusinessException.notFound("Story", nonExistentId));

            // When/Then
            mockMvc.perform(get("/api/v1/stories/{id}", nonExistentId)
                    .with(jwt().jwt(jwt -> jwt.subject(userId))))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/stories/{id}")
    class UpdateStoryEndpoint {

        @Test
        @DisplayName("Should update story successfully")
        void shouldUpdateStorySuccessfully() throws Exception {
            // Given
            UpdateStoryRequest request = new UpdateStoryRequest(
                "Updated Title",
                "Updated Description",
                "https://jira.example.com/PROJ-102"
            );

            when(storyService.updateStory(eq(storyId), any(), anyString(), anyString()))
                .thenReturn(storyResponse);

            // When/Then
            mockMvc.perform(put("/api/v1/stories/{id}", storyId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 409 when updating completed story")
        void shouldReturn409WhenUpdatingCompletedStory() throws Exception {
            // Given
            UpdateStoryRequest request = new UpdateStoryRequest("Title", "Description", null);

            when(storyService.updateStory(eq(storyId), any(), anyString(), anyString()))
                .thenThrow(BusinessException.conflict("Cannot update a completed story"));

            // When/Then
            mockMvc.perform(put("/api/v1/stories/{id}", storyId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/stories/{id}")
    class DeleteStoryEndpoint {

        @Test
        @DisplayName("Should delete story successfully")
        void shouldDeleteStorySuccessfully() throws Exception {
            // Given
            doNothing().when(storyService).deleteStory(eq(storyId), anyString(), anyString(), anyBoolean());

            // When/Then
            mockMvc.perform(delete("/api/v1/stories/{id}", storyId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName))))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 409 when deleting during voting")
        void shouldReturn409WhenDeletingDuringVoting() throws Exception {
            // Given
            doThrow(BusinessException.conflict("Cannot delete a story while voting is in progress"))
                .when(storyService).deleteStory(eq(storyId), anyString(), anyString(), anyBoolean());

            // When/Then
            mockMvc.perform(delete("/api/v1/stories/{id}", storyId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName))))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/stories/{id}/start-voting")
    class StartVotingEndpoint {

        @Test
        @DisplayName("Should start voting successfully")
        void shouldStartVotingSuccessfully() throws Exception {
            // Given
            StoryResponse votingStory = new StoryResponse(
                storyId, roomId, "User Authentication",
                "Implement OAuth2 login", null,
                StoryStatus.VOTING, null, null, 1, Instant.now(), null, Instant.now(), null
            );

            when(storyService.startVoting(eq(storyId), anyString(), anyString()))
                .thenReturn(votingStory);

            // When/Then
            mockMvc.perform(post("/api/v1/stories/{id}/start-voting", storyId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VOTING"));
        }

        @Test
        @DisplayName("Should return 409 when another story is in voting")
        void shouldReturn409WhenAnotherStoryInVoting() throws Exception {
            // Given
            when(storyService.startVoting(eq(storyId), anyString(), anyString()))
                .thenThrow(BusinessException.conflict("Another story is already in voting"));

            // When/Then
            mockMvc.perform(post("/api/v1/stories/{id}/start-voting", storyId)
                    .with(jwt().jwt(jwt -> jwt
                        .subject(userId)
                        .claim("preferred_username", userName))))
                .andExpect(status().isConflict());
        }
    }
}
