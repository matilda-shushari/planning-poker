package com.lufthansa.planning_poker.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoomUpdatedEvent extends BaseEvent {
    private UUID roomId;
    private String roomName;
    private String description;
    private String previousName;
}

