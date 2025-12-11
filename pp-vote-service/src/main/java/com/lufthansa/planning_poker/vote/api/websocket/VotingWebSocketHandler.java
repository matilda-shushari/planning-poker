package com.lufthansa.planning_poker.vote.api.websocket;

import com.lufthansa.planning_poker.vote.application.dto.response.VoteResultsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class VotingWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastVoteCount(UUID roomId, UUID storyId, int count) {
        String destination = "/topic/room/" + roomId + "/vote-count";
        Map<String, Object> payload = Map.of(
            "storyId", storyId,
            "voteCount", count,
            "type", "VOTE_COUNT_UPDATE"
        );
        log.debug("Broadcasting vote count to {}: {}", destination, payload);
        messagingTemplate.convertAndSend(destination, payload);
    }

    public void broadcastVoteResults(UUID roomId, VoteResultsResponse results) {
        String destination = "/topic/room/" + roomId + "/results";
        Map<String, Object> payload = Map.of(
            "storyId", results.storyId(),
            "type", "VOTES_REVEALED",
            "results", results
        );
        log.debug("Broadcasting vote results to {}", destination);
        messagingTemplate.convertAndSend(destination, payload);
    }

    public void broadcastVotingFinished(UUID roomId, UUID storyId, String finalEstimate, VoteResultsResponse results) {
        String destination = "/topic/room/" + roomId + "/finished";
        Map<String, Object> payload = Map.of(
            "storyId", storyId,
            "type", "VOTING_FINISHED",
            "finalEstimate", finalEstimate,
            "results", results
        );
        log.info("Broadcasting voting finished for story {} in room {}", storyId, roomId);
        messagingTemplate.convertAndSend(destination, payload);
    }

    public void broadcastVotesReset(UUID roomId, UUID storyId) {
        String destination = "/topic/room/" + roomId + "/reset";
        Map<String, Object> payload = Map.of(
            "storyId", storyId,
            "type", "VOTES_RESET"
        );
        log.debug("Broadcasting votes reset to {}", destination);
        messagingTemplate.convertAndSend(destination, payload);
    }

    public void broadcastVotingStarted(UUID roomId, UUID storyId, String storyTitle) {
        String destination = "/topic/room/" + roomId + "/voting-started";
        Map<String, Object> payload = Map.of(
            "storyId", storyId,
            "storyTitle", storyTitle,
            "type", "VOTING_STARTED"
        );
        log.info("Broadcasting voting started for story {} in room {}", storyId, roomId);
        messagingTemplate.convertAndSend(destination, payload);
    }

    public void broadcastUserJoined(UUID roomId, String oderId, String userName) {
        String destination = "/topic/room/" + roomId + "/participants";
        Map<String, Object> payload = Map.of(
            "userId", oderId,
            "userName", userName,
            "type", "USER_JOINED"
        );
        messagingTemplate.convertAndSend(destination, payload);
    }

    public void broadcastUserLeft(UUID roomId, String oderId, String userName) {
        String destination = "/topic/room/" + roomId + "/participants";
        Map<String, Object> payload = Map.of(
            "userId", oderId,
            "userName", userName,
            "type", "USER_LEFT"
        );
        messagingTemplate.convertAndSend(destination, payload);
    }
}

