package com.lufthansa.planning_poker.vote.application.service;

import com.lufthansa.planning_poker.vote.application.dto.request.CastVoteRequest;
import com.lufthansa.planning_poker.vote.application.dto.response.VoteResponse;
import com.lufthansa.planning_poker.vote.application.dto.response.VoteResultsResponse;
import com.lufthansa.planning_poker.vote.api.websocket.VotingWebSocketHandler;
import com.lufthansa.planning_poker.vote.infrastructure.messaging.VoteEventProducer;
import com.lufthansa.planning_poker.vote.infrastructure.persistence.entity.VoteEntity;
import com.lufthansa.planning_poker.vote.infrastructure.persistence.repository.JpaVoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoteService Tests")
class VoteServiceTest {

    @Mock
    private JpaVoteRepository voteRepository;

    @Mock
    private VoteEventProducer eventProducer;

    @Mock
    private VotingWebSocketHandler webSocketHandler;

    @InjectMocks
    private VoteService voteService;

    private UUID storyId;
    private UUID roomId;
    private UUID voteId;
    private String userId;
    private String userName;
    private VoteEntity voteEntity;

    @BeforeEach
    void setUp() {
        storyId = UUID.randomUUID();
        roomId = UUID.randomUUID();
        voteId = UUID.randomUUID();
        userId = "user-123";
        userName = "testuser";

        voteEntity = VoteEntity.builder()
            .id(voteId)
            .storyId(storyId)
            .roomId(roomId)
            .userId(userId)
            .userName(userName)
            .value("8")
            .createdAt(Instant.now())
            .build();
    }

    @Nested
    @DisplayName("castVote Tests")
    class CastVoteTests {

        @Test
        @DisplayName("Should cast new vote successfully")
        void shouldCastNewVoteSuccessfully() {
            // Given
            CastVoteRequest request = new CastVoteRequest(storyId, roomId, "8");

            when(voteRepository.findByStoryIdAndUserId(storyId, userId)).thenReturn(Optional.empty());
            when(voteRepository.save(any(VoteEntity.class))).thenReturn(voteEntity);
            when(voteRepository.countByStoryId(storyId)).thenReturn(1);

            // When
            VoteResponse result = voteService.castVote(request, userId, userName);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.value()).isEqualTo("8");
            verify(voteRepository).save(any(VoteEntity.class));
            verify(eventProducer).publishVoteCast(any());
            verify(webSocketHandler).broadcastVoteCount(roomId, storyId, 1);
        }

        @Test
        @DisplayName("Should update existing vote")
        void shouldUpdateExistingVote() {
            // Given
            CastVoteRequest request = new CastVoteRequest(storyId, roomId, "13");
            VoteEntity existingVote = VoteEntity.builder()
                .id(voteId)
                .storyId(storyId)
                .roomId(roomId)
                .userId(userId)
                .userName(userName)
                .value("8")
                .build();

            when(voteRepository.findByStoryIdAndUserId(storyId, userId)).thenReturn(Optional.of(existingVote));
            when(voteRepository.save(any(VoteEntity.class))).thenReturn(existingVote);
            when(voteRepository.countByStoryId(storyId)).thenReturn(1);

            // When
            VoteResponse result = voteService.castVote(request, userId, userName);

            // Then
            assertThat(result).isNotNull();
            ArgumentCaptor<VoteEntity> voteCaptor = ArgumentCaptor.forClass(VoteEntity.class);
            verify(voteRepository).save(voteCaptor.capture());
            assertThat(voteCaptor.getValue().getValue()).isEqualTo("13");
        }

        @Test
        @DisplayName("Should broadcast vote count after casting")
        void shouldBroadcastVoteCountAfterCasting() {
            // Given
            CastVoteRequest request = new CastVoteRequest(storyId, roomId, "5");

            when(voteRepository.findByStoryIdAndUserId(storyId, userId)).thenReturn(Optional.empty());
            when(voteRepository.save(any(VoteEntity.class))).thenReturn(voteEntity);
            when(voteRepository.countByStoryId(storyId)).thenReturn(3);

            // When
            voteService.castVote(request, userId, userName);

            // Then
            verify(webSocketHandler).broadcastVoteCount(roomId, storyId, 3);
        }

        @Test
        @DisplayName("Should publish vote cast event")
        void shouldPublishVoteCastEvent() {
            // Given
            CastVoteRequest request = new CastVoteRequest(storyId, roomId, "8");

            when(voteRepository.findByStoryIdAndUserId(storyId, userId)).thenReturn(Optional.empty());
            when(voteRepository.save(any(VoteEntity.class))).thenReturn(voteEntity);
            when(voteRepository.countByStoryId(storyId)).thenReturn(1);

            // When
            voteService.castVote(request, userId, userName);

            // Then
            verify(eventProducer).publishVoteCast(any());
        }
    }

    @Nested
    @DisplayName("getMyVote Tests")
    class GetMyVoteTests {

        @Test
        @DisplayName("Should return vote when found")
        void shouldReturnVoteWhenFound() {
            // Given
            when(voteRepository.findByStoryIdAndUserId(storyId, userId)).thenReturn(Optional.of(voteEntity));

            // When
            VoteResponse result = voteService.getMyVote(storyId, userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.value()).isEqualTo("8");
        }

        @Test
        @DisplayName("Should return null when vote not found")
        void shouldReturnNullWhenVoteNotFound() {
            // Given
            when(voteRepository.findByStoryIdAndUserId(storyId, userId)).thenReturn(Optional.empty());

            // When
            VoteResponse result = voteService.getMyVote(storyId, userId);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getVoteCount Tests")
    class GetVoteCountTests {

        @Test
        @DisplayName("Should return correct vote count")
        void shouldReturnCorrectVoteCount() {
            // Given
            when(voteRepository.countByStoryId(storyId)).thenReturn(5);

            // When
            int result = voteService.getVoteCount(storyId);

            // Then
            assertThat(result).isEqualTo(5);
        }

        @Test
        @DisplayName("Should return zero when no votes")
        void shouldReturnZeroWhenNoVotes() {
            // Given
            when(voteRepository.countByStoryId(storyId)).thenReturn(0);

            // When
            int result = voteService.getVoteCount(storyId);

            // Then
            assertThat(result).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("revealVotes Tests")
    class RevealVotesTests {

        @Test
        @DisplayName("Should reveal all votes for a story")
        void shouldRevealAllVotesForStory() {
            // Given
            List<VoteEntity> votes = List.of(
                createVoteEntity("user1", "User 1", "5"),
                createVoteEntity("user2", "User 2", "8"),
                createVoteEntity("user3", "User 3", "8")
            );

            when(voteRepository.findAllByStoryId(storyId)).thenReturn(votes);

            // When
            VoteResultsResponse result = voteService.revealVotes(storyId, roomId, userId, userName);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.totalVotes()).isEqualTo(3);
            assertThat(result.votes()).hasSize(3);
            verify(webSocketHandler).broadcastVoteResults(eq(roomId), any());
        }

        @Test
        @DisplayName("Should calculate average correctly")
        void shouldCalculateAverageCorrectly() {
            // Given
            List<VoteEntity> votes = List.of(
                createVoteEntity("user1", "User 1", "5"),
                createVoteEntity("user2", "User 2", "8"),
                createVoteEntity("user3", "User 3", "13")
            );

            when(voteRepository.findAllByStoryId(storyId)).thenReturn(votes);

            // When
            VoteResultsResponse result = voteService.revealVotes(storyId, roomId, userId, userName);

            // Then
            assertThat(result.averageScore()).isEqualByComparingTo(new BigDecimal("8.67"));
        }

        @Test
        @DisplayName("Should detect consensus when all votes are same")
        void shouldDetectConsensusWhenAllVotesSame() {
            // Given
            List<VoteEntity> votes = List.of(
                createVoteEntity("user1", "User 1", "8"),
                createVoteEntity("user2", "User 2", "8"),
                createVoteEntity("user3", "User 3", "8")
            );

            when(voteRepository.findAllByStoryId(storyId)).thenReturn(votes);

            // When
            VoteResultsResponse result = voteService.revealVotes(storyId, roomId, userId, userName);

            // Then
            assertThat(result.hasConsensus()).isTrue();
            assertThat(result.consensusValue()).isEqualTo("8");
        }

        @Test
        @DisplayName("Should not detect consensus when votes differ")
        void shouldNotDetectConsensusWhenVotesDiffer() {
            // Given
            List<VoteEntity> votes = List.of(
                createVoteEntity("user1", "User 1", "5"),
                createVoteEntity("user2", "User 2", "8")
            );

            when(voteRepository.findAllByStoryId(storyId)).thenReturn(votes);

            // When
            VoteResultsResponse result = voteService.revealVotes(storyId, roomId, userId, userName);

            // Then
            assertThat(result.hasConsensus()).isFalse();
            assertThat(result.consensusValue()).isNull();
        }

        @Test
        @DisplayName("Should handle non-numeric votes in average calculation")
        void shouldHandleNonNumericVotes() {
            // Given
            List<VoteEntity> votes = List.of(
                createVoteEntity("user1", "User 1", "5"),
                createVoteEntity("user2", "User 2", "?"),
                createVoteEntity("user3", "User 3", "8")
            );

            when(voteRepository.findAllByStoryId(storyId)).thenReturn(votes);

            // When
            VoteResultsResponse result = voteService.revealVotes(storyId, roomId, userId, userName);

            // Then
            assertThat(result.averageScore()).isEqualByComparingTo(new BigDecimal("6.50"));
        }
    }

    @Nested
    @DisplayName("finishVoting Tests")
    class FinishVotingTests {

        @Test
        @DisplayName("Should finish voting and publish event")
        void shouldFinishVotingAndPublishEvent() {
            // Given
            List<VoteEntity> votes = List.of(
                createVoteEntity("user1", "User 1", "8"),
                createVoteEntity("user2", "User 2", "8")
            );

            when(voteRepository.findAllByStoryId(storyId)).thenReturn(votes);

            // When
            VoteResultsResponse result = voteService.finishVoting(
                storyId, roomId, "8", "Test Story", userId, userName
            );

            // Then
            assertThat(result).isNotNull();
            verify(eventProducer).publishVotingFinished(any());
            verify(webSocketHandler).broadcastVotingFinished(eq(roomId), eq(storyId), eq("8"), any());
        }
    }

    @Nested
    @DisplayName("resetVotes Tests")
    class ResetVotesTests {

        @Test
        @DisplayName("Should delete all votes and broadcast reset")
        void shouldDeleteAllVotesAndBroadcastReset() {
            // When
            voteService.resetVotes(storyId, roomId);

            // Then
            verify(voteRepository).deleteAllByStoryId(storyId);
            verify(webSocketHandler).broadcastVotesReset(roomId, storyId);
        }
    }

    private VoteEntity createVoteEntity(String usrId, String usrName, String value) {
        return VoteEntity.builder()
            .id(UUID.randomUUID())
            .storyId(storyId)
            .roomId(roomId)
            .userId(usrId)
            .userName(usrName)
            .value(value)
            .createdAt(Instant.now())
            .build();
    }
}

