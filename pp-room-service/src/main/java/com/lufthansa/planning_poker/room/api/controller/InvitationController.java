package com.lufthansa.planning_poker.room.api.controller;

import com.lufthansa.planning_poker.room.application.dto.request.SendInviteRequest;
import com.lufthansa.planning_poker.room.application.dto.response.InviteResponse;
import com.lufthansa.planning_poker.room.application.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Invitations", description = "Email invitation management endpoints")
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/rooms/{roomId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send email invitation", 
               description = "Send an email invitation to join the room. Only the moderator can send invitations.")
    public InviteResponse sendInvitation(
            @PathVariable UUID roomId,
            @Valid @RequestBody SendInviteRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return invitationService.sendInvitation(
            roomId,
            request,
            jwt.getSubject(),
            jwt.getClaimAsString("preferred_username")
        );
    }

    @GetMapping("/rooms/{roomId}/invitations")
    @Operation(summary = "Get room invitations", 
               description = "Get all invitations for a room. Only the moderator can view invitations.")
    public List<InviteResponse> getInvitations(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal Jwt jwt) {
        return invitationService.getInvitationsByRoom(roomId, jwt.getSubject());
    }

    @PostMapping("/invitations/{token}/accept")
    @Operation(summary = "Accept invitation", 
               description = "Accept an email invitation and join the room")
    public InviteResponse acceptInvitation(
            @PathVariable String token,
            @AuthenticationPrincipal Jwt jwt) {
        return invitationService.acceptInvitation(
            token,
            jwt.getSubject(),
            jwt.getClaimAsString("preferred_username"),
            jwt.getClaimAsString("email")
        );
    }

    @DeleteMapping("/invitations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel invitation", 
               description = "Cancel a pending invitation. Only the moderator can cancel invitations.")
    public void cancelInvitation(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        invitationService.cancelInvitation(id, jwt.getSubject());
    }
}
