package com.lufthansa.planning_poker.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserJoinedRoomEvent extends BaseEvent {
    private UUID roomId;
    private String userId;
    private String userName;
    private String userEmail;
    private String joinMethod;  // INVITE_LINK, SHORT_CODE, EMAIL
}

