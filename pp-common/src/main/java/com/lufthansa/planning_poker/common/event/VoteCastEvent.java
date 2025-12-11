package com.lufthansa.planning_poker.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VoteCastEvent extends BaseEvent {
    private UUID voteId;
    private UUID storyId;
    private UUID roomId;
    private String userId;
    private String userName;
    private String value;
    private boolean isUpdate;  // true if user changed their vote
}

