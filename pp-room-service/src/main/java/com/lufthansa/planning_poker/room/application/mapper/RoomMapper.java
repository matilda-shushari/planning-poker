package com.lufthansa.planning_poker.room.application.mapper;

import com.lufthansa.planning_poker.room.application.dto.response.ParticipantResponse;
import com.lufthansa.planning_poker.room.application.dto.response.RoomResponse;
import com.lufthansa.planning_poker.room.application.dto.response.StoryResponse;
import com.lufthansa.planning_poker.room.infrastructure.persistence.entity.RoomEntity;
import com.lufthansa.planning_poker.room.infrastructure.persistence.entity.RoomParticipantEntity;
import com.lufthansa.planning_poker.room.infrastructure.persistence.entity.StoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    @Mapping(target = "inviteLink", expression = "java(\"/join/\" + entity.getShortCode())")
    @Mapping(target = "participantCount", expression = "java(entity.getParticipants() != null ? entity.getParticipants().size() : 0)")
    @Mapping(target = "storyCount", expression = "java(entity.getStories() != null ? entity.getStories().size() : 0)")
    @Mapping(target = "participants", source = "participants", qualifiedByName = "mapParticipants")
    @Mapping(target = "stories", source = "stories", qualifiedByName = "mapStories")
    RoomResponse toResponse(RoomEntity entity);

    @Mapping(target = "inviteLink", expression = "java(\"/join/\" + entity.getShortCode())")
    @Mapping(target = "participantCount", expression = "java(entity.getParticipants() != null ? entity.getParticipants().size() : 0)")
    @Mapping(target = "storyCount", expression = "java(entity.getStories() != null ? entity.getStories().size() : 0)")
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "stories", ignore = true)
    RoomResponse toResponseWithoutDetails(RoomEntity entity);

    @Named("mapParticipants")
    default List<ParticipantResponse> mapParticipants(Set<RoomParticipantEntity> participants) {
        if (participants == null) return Collections.emptyList();
        return participants.stream().map(this::toParticipantResponse).toList();
    }

    @Named("mapStories")
    default List<StoryResponse> mapStories(Set<StoryEntity> stories) {
        if (stories == null) return Collections.emptyList();
        return stories.stream().map(this::toStoryResponse).toList();
    }

    @Mapping(target = "roomId", source = "room.id")
    StoryResponse toStoryResponse(StoryEntity entity);

    ParticipantResponse toParticipantResponse(RoomParticipantEntity entity);
}

