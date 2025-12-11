package com.lufthansa.planning_poker.room.infrastructure.persistence.repository;

import com.lufthansa.planning_poker.room.infrastructure.persistence.entity.InvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaInvitationRepository extends JpaRepository<InvitationEntity, UUID> {

    Optional<InvitationEntity> findByToken(String token);

    List<InvitationEntity> findByRoomId(UUID roomId);

    List<InvitationEntity> findByRoomIdAndStatus(UUID roomId, InvitationEntity.InvitationStatus status);

    boolean existsByRoomIdAndEmailAndStatus(UUID roomId, String email, InvitationEntity.InvitationStatus status);
}

