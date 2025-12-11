package com.lufthansa.planning_poker.room.infrastructure.persistence.entity;

import com.lufthansa.planning_poker.room.domain.model.DeckType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "rooms")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class RoomEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeckType deckType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_deck_values", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "card_value", length = 20)
    @OrderColumn(name = "position")
    @Builder.Default
    private List<String> deckValues = new ArrayList<>();

    @Column(nullable = false, length = 100)
    @CreatedBy
    private String moderatorId;

    @Column(length = 100)
    private String moderatorName;

    @Column(unique = true, nullable = false, length = 8)
    private String shortCode;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("displayOrder ASC")
    private Set<StoryEntity> stories = new HashSet<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RoomParticipantEntity> participants = new HashSet<>();

    public void addStory(StoryEntity story) {
        stories.add(story);
        story.setRoom(this);
    }

    public void removeStory(StoryEntity story) {
        stories.remove(story);
        story.setRoom(null);
    }

    public void addParticipant(RoomParticipantEntity participant) {
        participants.add(participant);
        participant.setRoom(this);
    }
}

