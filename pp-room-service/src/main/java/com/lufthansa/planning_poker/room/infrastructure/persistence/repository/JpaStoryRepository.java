package com.lufthansa.planning_poker.room.infrastructure.persistence.repository;

import com.lufthansa.planning_poker.room.domain.model.StoryStatus;
import com.lufthansa.planning_poker.room.infrastructure.persistence.entity.StoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaStoryRepository extends JpaRepository<StoryEntity, UUID> {

    List<StoryEntity> findByRoomIdOrderByDisplayOrderAsc(UUID roomId);

    @Query("SELECT s FROM StoryEntity s WHERE s.room.id = :roomId AND s.status = :status")
    List<StoryEntity> findByRoomIdAndStatus(@Param("roomId") UUID roomId, @Param("status") StoryStatus status);

    @Query("SELECT s FROM StoryEntity s WHERE s.room.id = :roomId AND s.status = 'VOTING'")
    Optional<StoryEntity> findActiveVotingStory(@Param("roomId") UUID roomId);

    @Query("SELECT COALESCE(MAX(s.displayOrder), 0) + 1 FROM StoryEntity s WHERE s.room.id = :roomId")
    Integer getNextDisplayOrder(@Param("roomId") UUID roomId);

    long countByRoomId(UUID roomId);

    @Modifying
    @Query("UPDATE StoryEntity s SET s.status = :status WHERE s.id = :storyId")
    void updateStatus(@Param("storyId") UUID storyId, @Param("status") StoryStatus status);

    boolean existsByIdAndRoomModeratorId(UUID id, String moderatorId);

    @Query("SELECT s FROM StoryEntity s JOIN FETCH s.room ORDER BY s.createdAt DESC")
    Page<StoryEntity> findAllWithRoom(Pageable pageable);
}

