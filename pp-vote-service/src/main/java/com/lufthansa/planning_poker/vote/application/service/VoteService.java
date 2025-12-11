package com.lufthansa.planning_poker.vote.application.service;

import com.lufthansa.planning_poker.common.event.VoteCastEvent;
import com.lufthansa.planning_poker.common.event.VotingFinishedEvent;
import com.lufthansa.planning_poker.vote.application.dto.request.CastVoteRequest;
import com.lufthansa.planning_poker.vote.application.dto.response.VoteResponse;
import com.lufthansa.planning_poker.vote.application.dto.response.VoteResultsResponse;
import com.lufthansa.planning_poker.vote.infrastructure.messaging.VoteEventProducer;
import com.lufthansa.planning_poker.vote.infrastructure.persistence.entity.VoteEntity;
import com.lufthansa.planning_poker.vote.infrastructure.persistence.repository.JpaVoteRepository;
import com.lufthansa.planning_poker.vote.api.websocket.VotingWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing votes in Planning Poker sessions.
 * <p>
 * Handles vote casting, result calculation, and real-time updates via WebSocket.
 * Supports anonymous voting until reveal and consensus detection.
 * </p>
 *
 * @author Matilda Dervishaj
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VoteService {

    private final JpaVoteRepository voteRepository;
    private final VoteEventProducer eventProducer;
    private final VotingWebSocketHandler webSocketHandler;

    /**
     * Casts or updates a vote for a story.
     *
     * @param request  the vote request containing story ID and value
     * @param userId   the voting user's ID
     * @param userName the voting user's display name
     * @return the vote details
     */
    public VoteResponse castVote(CastVoteRequest request, String userId, String userName) {
        log.info("User {} casting vote {} for story {}", userId, request.value(), request.storyId());

        Optional<VoteEntity> existingVote = voteRepository.findByStoryIdAndUserId(request.storyId(), userId);
        boolean isUpdate = existingVote.isPresent();

        VoteEntity vote = existingVote.orElse(VoteEntity.builder()
            .storyId(request.storyId())
            .roomId(request.roomId())
            .userId(userId)
            .userName(userName)
            .build());

        vote.setValue(request.value());
        VoteEntity saved = voteRepository.save(vote);

        // Publish event
        VoteCastEvent event = VoteCastEvent.builder()
            .voteId(saved.getId())
            .storyId(saved.getStoryId())
            .roomId(saved.getRoomId())
            .userId(userId)
            .userName(userName)
            .value(saved.getValue())
            .isUpdate(isUpdate)
            .build();
        event.initialize(userId, userName);
        eventProducer.publishVoteCast(event);

        // Broadcast vote count (not values) via WebSocket
        int voteCount = voteRepository.countByStoryId(request.storyId());
        webSocketHandler.broadcastVoteCount(request.roomId(), request.storyId(), voteCount);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public VoteResponse getMyVote(UUID storyId, String userId) {
        return voteRepository.findByStoryIdAndUserId(storyId, userId)
            .map(this::toResponse)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public int getVoteCount(UUID storyId) {
        return voteRepository.countByStoryId(storyId);
    }

    /**
     * Reveals all votes for a story to participants.
     *
     * @param storyId  the story to reveal votes for
     * @param roomId   the room containing the story
     * @param userId   the requesting user's ID
     * @param userName the requesting user's display name
     * @return the vote results including all individual votes
     */
    public VoteResultsResponse revealVotes(UUID storyId, UUID roomId, String userId, String userName) {
        log.info("Revealing votes for story {} by {}", storyId, userId);

        List<VoteEntity> votes = voteRepository.findAllByStoryId(storyId);
        VoteResultsResponse results = calculateResults(storyId, roomId, votes);

        // Broadcast results via WebSocket
        webSocketHandler.broadcastVoteResults(roomId, results);

        return results;
    }

    /**
     * Finalizes voting for a story with a final estimate.
     *
     * @param storyId       the story to finalize
     * @param roomId        the room containing the story
     * @param finalEstimate the moderator's final estimate decision
     * @param storyTitle    the story title for event logging
     * @param userId        the moderator's ID
     * @param userName      the moderator's display name
     * @return the final vote results
     */
    public VoteResultsResponse finishVoting(UUID storyId, UUID roomId, String finalEstimate, 
                                            String storyTitle, String userId, String userName) {
        log.info("Finishing voting for story {} by {}", storyId, userId);

        List<VoteEntity> votes = voteRepository.findAllByStoryId(storyId);
        VoteResultsResponse results = calculateResults(storyId, roomId, votes);

        // Publish finished event
        VotingFinishedEvent event = VotingFinishedEvent.builder()
            .storyId(storyId)
            .roomId(roomId)
            .storyTitle(storyTitle)
            .averageScore(results.averageScore())
            .finalEstimate(finalEstimate)
            .totalVotes(results.totalVotes())
            .votes(votes.stream()
                .map(v -> new VotingFinishedEvent.VoteSummary(v.getUserId(), v.getUserName(), v.getValue()))
                .toList())
            .build();
        event.initialize(userId, userName);
        eventProducer.publishVotingFinished(event);

        // Broadcast final results via WebSocket
        webSocketHandler.broadcastVotingFinished(roomId, storyId, finalEstimate, results);

        return results;
    }

    public void resetVotes(UUID storyId, UUID roomId) {
        log.info("Resetting votes for story {}", storyId);
        voteRepository.deleteAllByStoryId(storyId);
        webSocketHandler.broadcastVotesReset(roomId, storyId);
    }

    private VoteResultsResponse calculateResults(UUID storyId, UUID roomId, List<VoteEntity> votes) {
        List<VoteResultsResponse.VoteDetail> voteDetails = votes.stream()
            .map(v -> new VoteResultsResponse.VoteDetail(v.getUserId(), v.getUserName(), v.getValue()))
            .toList();

        BigDecimal average = calculateAverage(votes);
        String consensusValue = findConsensus(votes);
        boolean hasConsensus = consensusValue != null;

        return new VoteResultsResponse(
            storyId,
            roomId,
            votes.size(),
            average,
            consensusValue,
            hasConsensus,
            voteDetails
        );
    }

    private BigDecimal calculateAverage(List<VoteEntity> votes) {
        List<BigDecimal> numericVotes = votes.stream()
            .map(VoteEntity::getValue)
            .filter(this::isNumeric)
            .map(BigDecimal::new)
            .toList();

        if (numericVotes.isEmpty()) {
            return null;
        }

        BigDecimal sum = numericVotes.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(numericVotes.size()), 2, RoundingMode.HALF_UP);
    }

    private String findConsensus(List<VoteEntity> votes) {
        if (votes.isEmpty()) return null;

        Map<String, Long> valueCounts = votes.stream()
            .collect(Collectors.groupingBy(VoteEntity::getValue, Collectors.counting()));

        // If all votes are the same value
        if (valueCounts.size() == 1) {
            return valueCounts.keySet().iterator().next();
        }

        return null;
    }

    private boolean isNumeric(String value) {
        try {
            new BigDecimal(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private VoteResponse toResponse(VoteEntity entity) {
        return new VoteResponse(
            entity.getId(),
            entity.getStoryId(),
            entity.getRoomId(),
            entity.getUserId(),
            entity.getUserName(),
            entity.getValue(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}

