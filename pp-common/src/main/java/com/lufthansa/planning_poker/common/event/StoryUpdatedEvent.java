package com.lufthansa.planning_poker.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StoryUpdatedEvent extends BaseEvent {
    private UUID storyId;
    private UUID roomId;
    private String title;
    private String description;
    private String jiraLink;
    private String previousTitle;
}

