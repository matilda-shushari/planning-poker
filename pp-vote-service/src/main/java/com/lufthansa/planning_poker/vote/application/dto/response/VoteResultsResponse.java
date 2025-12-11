package com.lufthansa.planning_poker.vote.application.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VoteResultsResponse(
    UUID storyId,
    UUID roomId,
    int totalVotes,
    BigDecimal averageScore,
    String consensusValue,
    boolean hasConsensus,
    List<VoteDetail> votes
) {
    public record VoteDetail(
        String userId,
        String userName,
        String value
    ) {}
}

