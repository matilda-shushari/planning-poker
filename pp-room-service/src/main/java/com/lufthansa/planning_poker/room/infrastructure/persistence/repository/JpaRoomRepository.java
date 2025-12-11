package com.lufthansa.planning_poker.room.infrastructure.persistence.repository;

import com.lufthansa.planning_poker.room.infrastructure.persistence.entity.RoomEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaRoomRepository extends JpaRepository<RoomEntity, UUID> {

    Optional<RoomEntity> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    Page<RoomEntity> findByModeratorId(String moderatorId, Pageable pageable);

    @Query("SELECT r FROM RoomEntity r WHERE r.active = true AND r.moderatorId = :userId")
    Page<RoomEntity> findActiveRoomsByModerator(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT r FROM RoomEntity r JOIN r.participants p WHERE p.userId = :userId AND r.active = true")
    Page<RoomEntity> findRoomsWhereUserIsParticipant(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT r FROM RoomEntity r LEFT JOIN FETCH r.stories LEFT JOIN FETCH r.participants WHERE r.id = :id")
    Optional<RoomEntity> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT r FROM RoomEntity r LEFT JOIN FETCH r.stories LEFT JOIN FETCH r.participants WHERE r.shortCode = :shortCode")
    Optional<RoomEntity> findByShortCodeWithDetails(@Param("shortCode") String shortCode);

    @Query("SELECT r FROM RoomEntity r WHERE r.active = true")
    Page<RoomEntity> findAllActiveRooms(Pageable pageable);
}

