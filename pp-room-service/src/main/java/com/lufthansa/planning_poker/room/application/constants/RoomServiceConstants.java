package com.lufthansa.planning_poker.room.application.constants;

/**
 * Constants used across the Room Service module.
 * <p>
 * Centralizes all string literals, entity names, and error messages
 * to avoid duplication and improve maintainability.
 * </p>
 *
 * @author Matilda Dervishaj
 * @since 1.0
 */
public final class RoomServiceConstants {

    private RoomServiceConstants() {
        // Prevent instantiation
    }

    /**
     * Entity names for exception messages
     */
    public static final String ENTITY_ROOM = "Room";
    public static final String ENTITY_STORY = "Story";
    public static final String ENTITY_INVITATION = "Invitation";

    /**
     * Room-related error messages
     */
    public static final String ERR_ROOM_NOT_ACTIVE = "This room is no longer active";
    public static final String ERR_ONLY_MODERATOR_UPDATE_ROOM = "Only the moderator can update this room";
    public static final String ERR_ONLY_MODERATOR_DELETE_ROOM = "Only the moderator or admin can delete this room";

    /**
     * Story-related error messages
     */
    public static final String ERR_ONLY_MODERATOR_CREATE_STORY = "Only the moderator can create stories";
    public static final String ERR_ONLY_MODERATOR_UPDATE_STORY = "Only the moderator can update stories";
    public static final String ERR_ONLY_MODERATOR_DELETE_STORY = "Only the moderator can delete stories";
    public static final String ERR_ONLY_MODERATOR_START_VOTING = "Only the moderator can start voting";
    public static final String ERR_CANNOT_UPDATE_COMPLETED = "Cannot update a completed story";
    public static final String ERR_CANNOT_DELETE_DURING_VOTING = "Cannot delete a story while voting is in progress";

    /**
     * Invitation-related error messages
     */
    public static final String ERR_ONLY_MODERATOR_SEND_INVITE = "Only the moderator can send invitations";
    public static final String ERR_ONLY_MODERATOR_VIEW_INVITE = "Only the moderator can view invitations";
    public static final String ERR_ONLY_MODERATOR_CANCEL_INVITE = "Only the moderator can cancel invitations";
    public static final String ERR_INVITATION_ALREADY_PENDING = "An invitation is already pending for this email";
    public static final String ERR_ONLY_CANCEL_PENDING = "Can only cancel pending invitations";

    /**
     * Short code generation
     */
    public static final String SHORT_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    public static final int SHORT_CODE_LENGTH = 6;

    /**
     * Cache names
     */
    public static final String CACHE_ROOMS = "rooms";
    public static final String CACHE_STORIES = "stories";
}

