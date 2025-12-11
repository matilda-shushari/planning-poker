package com.lufthansa.planning_poker.room.api.controller;

import com.lufthansa.planning_poker.room.application.dto.request.CreateRoomRequest;
import com.lufthansa.planning_poker.room.application.dto.request.UpdateRoomRequest;
import com.lufthansa.planning_poker.room.application.dto.response.RoomResponse;
import com.lufthansa.planning_poker.room.application.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms", description = "Room management endpoints")
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new room", description = "Creates a new planning poker room. The creator becomes the moderator.")
    public RoomResponse createRoom(
            @Valid @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return roomService.createRoom(
            request,
            jwt.getSubject(),
            jwt.getClaimAsString("preferred_username")
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get room by ID")
    public RoomResponse getRoom(@PathVariable UUID id) {
        return roomService.getRoomById(id);
    }

    @GetMapping("/code/{shortCode}")
    @Operation(summary = "Get room by short code", description = "Used for joining via short invite links")
    public RoomResponse getRoomByShortCode(@PathVariable String shortCode) {
        return roomService.getRoomByShortCode(shortCode);
    }

    @GetMapping("/my-rooms")
    @Operation(summary = "Get rooms where user is moderator")
    public Page<RoomResponse> getMyRooms(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable) {
        return roomService.getMyRooms(jwt.getSubject(), pageable);
    }

    @GetMapping("/joined")
    @Operation(summary = "Get rooms where user is participant")
    public Page<RoomResponse> getJoinedRooms(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable) {
        return roomService.getJoinedRooms(jwt.getSubject(), pageable);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update room", description = "Only the moderator can update the room")
    public RoomResponse updateRoom(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoomRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return roomService.updateRoom(
            id,
            request,
            jwt.getSubject(),
            jwt.getClaimAsString("preferred_username")
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete room", description = "Only the moderator or admin can delete the room")
    public void deleteRoom(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        boolean isAdmin = hasRole(jwt, "ADMIN");
        roomService.deleteRoom(
            id,
            jwt.getSubject(),
            jwt.getClaimAsString("preferred_username"),
            isAdmin
        );
    }

    @PostMapping("/join/{shortCode}")
    @Operation(summary = "Join room via short code")
    public RoomResponse joinRoom(
            @PathVariable String shortCode,
            @AuthenticationPrincipal Jwt jwt) {
        return roomService.joinRoom(
            shortCode,
            jwt.getSubject(),
            jwt.getClaimAsString("preferred_username"),
            jwt.getClaimAsString("email")
        );
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all rooms (Admin only)")
    public Page<RoomResponse> getAllRooms(@PageableDefault(size = 20) Pageable pageable) {
        return roomService.getAllRooms(pageable);
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
