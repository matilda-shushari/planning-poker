package com.lufthansa.planning_poker.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VotingFinishedEvent extends BaseEvent {
    private UUID storyId;
    private UUID roomId;
    private String storyTitle;
    private BigDecimal averageScore;
    private String finalEstimate;
    private int totalVotes;
    private List<VoteSummary> votes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VoteSummary {
        private String userId;
        private String userName;
        private String value;
    }
}

