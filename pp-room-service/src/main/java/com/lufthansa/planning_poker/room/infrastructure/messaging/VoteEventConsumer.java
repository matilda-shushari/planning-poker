package com.lufthansa.planning_poker.room.infrastructure.messaging;

import com.lufthansa.planning_poker.common.event.BaseEvent;
import com.lufthansa.planning_poker.common.event.KafkaTopics;
import com.lufthansa.planning_poker.common.event.VotingFinishedEvent;
import com.lufthansa.planning_poker.room.domain.model.StoryStatus;
import com.lufthansa.planning_poker.room.infrastructure.persistence.repository.JpaStoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Consumes vote events from Kafka to update story status.
 * <p>
 * When voting is finished in Vote Service, this consumer
 * updates the story status to COMPLETED in Room Service.
 * </p>
 *
 * @author Matilda Dervishaj
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VoteEventConsumer {

    private final JpaStoryRepository storyRepository;

    @KafkaListener(
        topics = KafkaTopics.VOTE_EVENTS,
        groupId = "room-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeVoteEvents(BaseEvent event) {
        if (event instanceof VotingFinishedEvent votingFinished) {
            handleVotingFinished(votingFinished);
        }
    }

    private void handleVotingFinished(VotingFinishedEvent event) {
        log.info("Received VotingFinishedEvent for story: {}", event.getStoryId());
        
        storyRepository.findById(event.getStoryId())
            .ifPresentOrElse(
                story -> {
                    story.setStatus(StoryStatus.COMPLETED);
                    story.setFinalEstimate(event.getFinalEstimate());
                    story.setAverageScore(event.getAverageScore());
                    story.setVotingEndedAt(Instant.now());
                    storyRepository.save(story);
                    log.info("Story {} marked as COMPLETED with estimate: {}", 
                        event.getStoryId(), event.getFinalEstimate());
                },
                () -> log.warn("Story not found for VotingFinishedEvent: {}", event.getStoryId())
            );
    }
}

