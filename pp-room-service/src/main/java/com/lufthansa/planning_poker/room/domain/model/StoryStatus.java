package com.lufthansa.planning_poker.room.domain.model;

/**
 * Status of a story in the voting process.
 */
public enum StoryStatus {
    PENDING,    // Not yet started
    VOTING,     // Voting in progress
    REVEALED,   // Votes revealed, can still re-vote
    COMPLETED   // Final, locked - no more changes allowed
}

