package com.lufthansa.planning_poker.vote.infrastructure.messaging;

import com.lufthansa.planning_poker.common.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteEventProducer {

    private final KafkaTemplate<String, BaseEvent> kafkaTemplate;

    public void publishVoteCast(VoteCastEvent event) {
        log.info("Publishing VoteCastEvent for story: {}", event.getStoryId());
        kafkaTemplate.send(KafkaTopics.VOTE_EVENTS, event.getStoryId().toString(), event);
    }

    public void publishVotingStarted(VotingStartedEvent event) {
        log.info("Publishing VotingStartedEvent for story: {}", event.getStoryId());
        kafkaTemplate.send(KafkaTopics.VOTE_EVENTS, event.getRoomId().toString(), event);
    }

    public void publishVotingFinished(VotingFinishedEvent event) {
        log.info("Publishing VotingFinishedEvent for story: {}", event.getStoryId());
        kafkaTemplate.send(KafkaTopics.VOTE_EVENTS, event.getRoomId().toString(), event);
    }
}

