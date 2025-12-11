package com.lufthansa.planning_poker.room.domain.model;

/**
 * Role of a participant in a Planning Poker room.
 * <p>
 * Based on requirements:
 * - MODERATOR: Room creator who can manage stories and control voting
 * - VOTER: Regular participant who can cast votes on active stories
 * </p>
 *
 * @author Matilda Dervishaj
 * @since 1.0
 */
public enum ParticipantRole {
    
    /**
     * Room creator with full control.
     * Can create/update/delete stories, start/finish voting, and invite participants.
     */
    MODERATOR,
    
    /**
     * Regular participant who can cast votes on active stories.
     */
    VOTER
}

