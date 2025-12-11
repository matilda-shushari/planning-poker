package com.lufthansa.planning_poker.room.api.controller;

import com.lufthansa.planning_poker.room.application.dto.request.CreateStoryRequest;
import com.lufthansa.planning_poker.room.application.dto.request.UpdateStoryRequest;
import com.lufthansa.planning_poker.room.application.dto.response.StoryResponse;
import com.lufthansa.planning_poker.room.application.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Stories", description = "Story management endpoints")
public class StoryController {

    private final StoryService storyService;

    @PostMapping("/rooms/{roomId}/stories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new story", description = "Only the room moderator can create stories")
    public StoryResponse createStory(
            @PathVariable UUID roomId,
            @Valid @RequestBody CreateStoryRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return storyService.createStory(
            roomId,
            request,
            jwt.getSubject(),
            jwt.getClaimAsString("preferred_username")
        );
    }

    @GetMapping("/rooms/{roomId}/stories")
    @Operation(summary = "Get all stories for a room")
    public List<StoryResponse> getStoriesByRoom(@PathVariable UUID roomId) {
        return storyService.getStoriesByRoomId(roomId);
    }

    @GetMapping("/stories/{id}")
    @Operation(summary = "Get story by ID")
    public StoryResponse getStory(@PathVariable UUID id) {
        return storyService.getStoryById(id);
    }

    @PutMapping("/stories/{id}")
    @Operation(summary = "Update story", description = "Only the room moderator can update stories")
    public StoryResponse updateStory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStoryRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return storyService.updateStory(
            id,
            request,
            jwt.getSubject(),
            jwt.getClaimAsString("preferred_username")
        );
    }

    @DeleteMapping("/stories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete story", description = "Only the room moderator or admin can delete stories")
    public void deleteStory(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        boolean isAdmin = hasRole(jwt, "ADMIN");
        storyService.deleteStory(
            id,
            jwt.getSubject(),
            jwt.getClaimAsString("preferred_username"),
            isAdmin
        );
    }

    @PostMapping("/stories/{id}/start-voting")
    @Operation(summary = "Start voting for a story", description = "Only the room moderator can start voting")
    public StoryResponse startVoting(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        return storyService.startVoting(
            id,
            jwt.getSubject(),
            jwt.getClaimAsString("preferred_username")
        );
    }

    @GetMapping("/admin/stories")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all stories (Admin only)", description = "View all stories across rooms for auditing purposes")
    public Page<StoryResponse> getAllStories(@PageableDefault(size = 50) Pageable pageable) {
        return storyService.getAllStories(pageable);
    }

    private boolean hasRole(Jwt jwt, String role) {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            var roles = (java.util.List<?>) realmAccess.get("roles");
            return roles.contains(role);
        }
        return false;
    }
}
