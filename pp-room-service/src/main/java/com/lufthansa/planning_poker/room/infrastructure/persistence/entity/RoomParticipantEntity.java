package com.lufthansa.planning_poker.room.infrastructure.persistence.entity;

import com.lufthansa.planning_poker.room.domain.model.ParticipantRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "room_participants",
       uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"}))
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class RoomParticipantEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(length = 100)
    private String userName;

    @Column(length = 255)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ParticipantRole role = ParticipantRole.VOTER;

    @Column(nullable = false)
    @Builder.Default
    private boolean online = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    private Instant lastSeenAt;
}

