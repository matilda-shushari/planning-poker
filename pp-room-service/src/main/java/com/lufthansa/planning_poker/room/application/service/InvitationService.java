package com.lufthansa.planning_poker.room.application.service;

import com.lufthansa.planning_poker.room.api.exception.BusinessException;
import com.lufthansa.planning_poker.room.application.constants.RoomServiceConstants;
import com.lufthansa.planning_poker.room.application.dto.request.SendInviteRequest;
import com.lufthansa.planning_poker.room.application.dto.response.InviteResponse;
import com.lufthansa.planning_poker.room.infrastructure.persistence.entity.InvitationEntity;
import com.lufthansa.planning_poker.room.infrastructure.persistence.entity.RoomEntity;
import com.lufthansa.planning_poker.room.infrastructure.persistence.repository.JpaInvitationRepository;
import com.lufthansa.planning_poker.room.infrastructure.persistence.repository.JpaRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing room invitations.
 * <p>
 * Handles sending, accepting, and cancelling email invitations to rooms.
 * Generates secure tokens for invitation links with configurable expiry.
 * </p>
 *
 * @author Matilda Dervishaj
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvitationService {

    private final JpaInvitationRepository invitationRepository;
    private final JpaRoomRepository roomRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.base-url:http://localhost:4200}")
    private String baseUrl;

    @Value("${app.invitation.expiry-hours:48}")
    private int expiryHours;

    /**
     * Sends an invitation email to join a room.
     *
     * @param roomId        the room to invite to
     * @param request       the invitation request containing email
     * @param invitedBy     the inviting user's ID (must be moderator)
     * @param invitedByName the inviting user's display name
     * @return the invitation details including secure link
     * @throws BusinessException if room not found or invitation already pending
     */
    public InviteResponse sendInvitation(UUID roomId, SendInviteRequest request, String invitedBy, String invitedByName) {
        RoomEntity room = roomRepository.findById(roomId)
            .orElseThrow(() -> BusinessException.notFound(RoomServiceConstants.ENTITY_ROOM, roomId));

        // Only moderator can send invitations
        if (!room.getModeratorId().equals(invitedBy)) {
            throw BusinessException.forbidden(RoomServiceConstants.ERR_ONLY_MODERATOR_SEND_INVITE);
        }

        // Check if pending invitation already exists
        if (invitationRepository.existsByRoomIdAndEmailAndStatus(
                roomId, request.email(), InvitationEntity.InvitationStatus.PENDING)) {
            throw BusinessException.conflict(RoomServiceConstants.ERR_INVITATION_ALREADY_PENDING);
        }

        // Generate secure token
        String token = generateSecureToken();
        String inviteLink = baseUrl + "/join/invite/" + token;

        // Create invitation
        InvitationEntity invitation = InvitationEntity.builder()
            .room(room)
            .email(request.email())
            .token(token)
            .status(InvitationEntity.InvitationStatus.PENDING)
            .expiresAt(Instant.now().plus(expiryHours, ChronoUnit.HOURS))
            .invitedBy(invitedByName)
            .build();

        InvitationEntity saved = invitationRepository.save(invitation);

        // Log the invitation (in production, this would send an actual email)
        log.info("📧 EMAIL INVITATION SENT:");
        log.info("   To: {}", request.email());
        log.info("   Room: {} ({})", room.getName(), room.getShortCode());
        log.info("   Invited by: {}", invitedByName);
        log.info("   Join link: {}", inviteLink);
        log.info("   Expires: {}", saved.getExpiresAt());

        // TODO: Integrate with actual email service (SendGrid, AWS SES, SMTP, etc.)
        // emailService.sendInvitationEmail(request.email(), room.getName(), inviteLink, invitedByName);

        return new InviteResponse(
            saved.getId(),
            roomId,
            room.getName(),
            request.email(),
            saved.getStatus().name(),
            inviteLink,
            saved.getExpiresAt(),
            saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> getInvitationsByRoom(UUID roomId, String userId) {
        RoomEntity room = roomRepository.findById(roomId)
            .orElseThrow(() -> BusinessException.notFound(RoomServiceConstants.ENTITY_ROOM, roomId));

        if (!room.getModeratorId().equals(userId)) {
            throw BusinessException.forbidden(RoomServiceConstants.ERR_ONLY_MODERATOR_VIEW_INVITE);
        }

        return invitationRepository.findByRoomId(roomId).stream()
            .map(inv -> new InviteResponse(
                inv.getId(),
                roomId,
                room.getName(),
                inv.getEmail(),
                inv.getStatus().name(),
                baseUrl + "/join/invite/" + inv.getToken(),
                inv.getExpiresAt(),
                inv.getCreatedAt()
            ))
            .toList();
    }

    public InviteResponse acceptInvitation(String token, String userId, String userName, String userEmail) {
        InvitationEntity invitation = invitationRepository.findByToken(token)
            .orElseThrow(() -> BusinessException.notFound(RoomServiceConstants.ENTITY_INVITATION, token));

        if (invitation.getStatus() != InvitationEntity.InvitationStatus.PENDING) {
            throw BusinessException.conflict("This invitation has already been " + invitation.getStatus().name().toLowerCase());
        }

        if (invitation.isExpired()) {
            invitation.setStatus(InvitationEntity.InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw BusinessException.conflict("This invitation has expired");
        }

        // Mark invitation as accepted
        invitation.setStatus(InvitationEntity.InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        invitationRepository.save(invitation);

        RoomEntity room = invitation.getRoom();
        log.info("Invitation accepted by {} for room {}", userName, room.getName());

        return new InviteResponse(
            invitation.getId(),
            room.getId(),
            room.getName(),
            invitation.getEmail(),
            invitation.getStatus().name(),
            null,
            invitation.getExpiresAt(),
            invitation.getCreatedAt()
        );
    }

    public void cancelInvitation(UUID invitationId, String userId) {
        InvitationEntity invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> BusinessException.notFound(RoomServiceConstants.ENTITY_INVITATION, invitationId));

        if (!invitation.getRoom().getModeratorId().equals(userId)) {
            throw BusinessException.forbidden(RoomServiceConstants.ERR_ONLY_MODERATOR_CANCEL_INVITE);
        }

        if (invitation.getStatus() != InvitationEntity.InvitationStatus.PENDING) {
            throw BusinessException.conflict(RoomServiceConstants.ERR_ONLY_CANCEL_PENDING);
        }

        invitation.setStatus(InvitationEntity.InvitationStatus.CANCELLED);
        invitationRepository.save(invitation);
        log.info("Invitation {} cancelled", invitationId);
    }

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}

