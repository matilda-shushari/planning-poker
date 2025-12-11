package com.lufthansa.planning_poker.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoomCreatedEvent extends BaseEvent {
    private UUID roomId;
    private String roomName;
    private String description;
    private String deckType;
    private List<String> deckValues;
    private String shortCode;
    private String moderatorId;
    private String moderatorName;
}

