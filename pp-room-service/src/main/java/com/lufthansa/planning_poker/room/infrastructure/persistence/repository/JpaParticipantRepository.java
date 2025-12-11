package com.lufthansa.planning_poker.room.infrastructure.persistence.repository;

import com.lufthansa.planning_poker.room.infrastructure.persistence.entity.RoomParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaParticipantRepository extends JpaRepository<RoomParticipantEntity, UUID> {

    Optional<RoomParticipantEntity> findByRoomIdAndUserId(UUID roomId, String userId);

    List<RoomParticipantEntity> findByRoomId(UUID roomId);

    boolean existsByRoomIdAndUserId(UUID roomId, String userId);

    int countByRoomId(UUID roomId);

    @Modifying
    @Query("UPDATE RoomParticipantEntity p SET p.online = :online WHERE p.room.id = :roomId AND p.userId = :userId")
    void updateOnlineStatus(@Param("roomId") UUID roomId, @Param("userId") String userId, @Param("online") boolean online);

    @Modifying
    @Query("DELETE FROM RoomParticipantEntity p WHERE p.room.id = :roomId AND p.userId = :userId")
    void removeFromRoom(@Param("roomId") UUID roomId, @Param("userId") String userId);
}

