package com.lufthansa.planning_poker.vote.infrastructure.persistence.repository;

import com.lufthansa.planning_poker.vote.infrastructure.persistence.entity.VoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaVoteRepository extends JpaRepository<VoteEntity, UUID> {

    Optional<VoteEntity> findByStoryIdAndUserId(UUID storyId, String oderId);

    List<VoteEntity> findAllByStoryId(UUID storyId);

    int countByStoryId(UUID storyId);

    boolean existsByStoryIdAndUserId(UUID storyId, String oderId);

    @Modifying
    @Query("DELETE FROM VoteEntity v WHERE v.storyId = :storyId")
    void deleteAllByStoryId(@Param("storyId") UUID storyId);

    @Modifying
    @Query("DELETE FROM VoteEntity v WHERE v.roomId = :roomId")
    void deleteAllByRoomId(@Param("roomId") UUID roomId);
}

