package com.lufthansa.planning_poker.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VotingStartedEvent extends BaseEvent {
    private UUID storyId;
    private UUID roomId;
    private String storyTitle;
}

