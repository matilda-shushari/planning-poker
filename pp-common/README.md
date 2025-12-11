# 📦 Common Library

**Planning Poker Platform - Shared Components & Event DTOs**

---

## 🚀 Quick Start

### Building the Library

```bash
# From project root directory
cd planning-poker

# Build and install to local Maven repository
.\mvnw install -pl pp-common

# Or build all modules (includes pp-common)
.\mvnw clean install
```

### Verify Installation

```bash
# Check if installed in local Maven repository
dir %USERPROFILE%\.m2\repository\com\lufthansa\pp-common\
```

### Using in Other Services

This module is automatically included as a dependency in all microservices.
No manual configuration needed.

---

## 📖 Overview

The Common module contains shared code used across all microservices. This primarily includes Kafka event DTOs that define the contract for inter-service communication, ensuring type-safe event handling across the platform.

## Purpose

| Responsibility | Description |
|----------------|-------------|
| **Event DTOs** | Shared event classes for Kafka communication |
| **Type Safety** | Ensures producers and consumers use same event structure |
| **DRY Principle** | Avoids duplicate event definitions across services |
| **Contract Definition** | Single source of truth for event schemas |

---

## 🔄 Event Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     KAFKA EVENT FLOW                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Room Service                                                   │
│       │                                                         │
│       │  uses pp-common                                         │
│       │  RoomCreatedEvent                                       │
│       ▼                                                         │
│  ┌─────────────┐                                                │
│  │   Kafka     │◄────── Shared Event DTOs ──────►│              │
│  │   Topics    │         (pp-common)             │              │
│  └─────────────┘                                                │
│       │                                                         │
│       │  uses pp-common                                         │
│       │  RoomCreatedEvent                                       │
│       ▼                                                         │
│  Audit Service                                                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📋 Event Classes

### Base Event

All events extend the `BaseEvent` class:

```java
public abstract class BaseEvent {
    private String eventId;        // Unique event identifier
    private String eventType;      // Event type name
    private Instant timestamp;     // When event occurred
    private String userId;         // User who triggered event
    private String userDisplayName; // User's display name
}
```

### Room Events

| Event Class | Description | Fields |
|-------------|-------------|--------|
| `RoomCreatedEvent` | New room created | roomId, roomName, deckType, moderatorId |
| `RoomUpdatedEvent` | Room modified | roomId, roomName, changes |
| `RoomDeletedEvent` | Room deleted | roomId, roomName, deletedBy |
| `UserJoinedRoomEvent` | User joined room | roomId, participantId, role |
| `UserLeftRoomEvent` | User left room | roomId, participantId |

### Story Events

| Event Class | Description | Fields |
|-------------|-------------|--------|
| `StoryCreatedEvent` | Story added | storyId, roomId, title |
| `StoryUpdatedEvent` | Story modified | storyId, changes |
| `StoryDeletedEvent` | Story removed | storyId, roomId |
| `VotingStartedEvent` | Voting opened | storyId, roomId |

### Vote Events

| Event Class | Description | Fields |
|-------------|-------------|--------|
| `VoteCastEvent` | Vote submitted | voteId, storyId, value |
| `VoteUpdatedEvent` | Vote changed | voteId, oldValue, newValue |
| `VotesRevealedEvent` | Votes revealed | storyId, votes, average |
| `VotingFinishedEvent` | Voting completed | storyId, finalEstimate, average |

---

## 📨 Kafka Topics

| Topic | Events | Producers | Consumers |
|-------|--------|-----------|-----------|
| `room-events` | Room & User events | Room Service | Audit Service |
| `story-events` | Story & Voting events | Room Service | Audit Service |
| `vote-events` | Vote events | Vote Service | Audit Service |

---

## 💻 Usage Examples

### Publishing Events (Producer)

```java
@Service
public class RoomEventProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishRoomCreated(Room room, String userId, String userName) {
        RoomCreatedEvent event = RoomCreatedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("ROOM_CREATED")
            .timestamp(Instant.now())
            .userId(userId)
            .userDisplayName(userName)
            .roomId(room.getId().toString())
            .roomName(room.getName())
            .deckType(room.getDeckType().name())
            .build();
            
        kafkaTemplate.send("room-events", event);
    }
}
```

### Consuming Events (Consumer)

```java
@Service
public class AuditEventConsumer {
    
    @KafkaListener(topics = "room-events")
    public void handleRoomEvent(RoomCreatedEvent event) {
        AuditLog log = AuditLog.builder()
            .action("ROOM_CREATED")
            .entityType("ROOM")
            .entityId(event.getRoomId())
            .userId(event.getUserId())
            .userDisplayName(event.getUserDisplayName())
            .timestamp(event.getTimestamp())
            .build();
            
        auditLogRepository.save(log);
    }
}
```

---

## 📁 Project Structure

```
pp-common/
├── src/main/java/com/lufthansa/planning_poker/common/
│   └── event/
│       ├── BaseEvent.java
│       ├── room/
│       │   ├── RoomCreatedEvent.java
│       │   ├── RoomUpdatedEvent.java
│       │   ├── RoomDeletedEvent.java
│       │   ├── UserJoinedRoomEvent.java
│       │   └── UserLeftRoomEvent.java
│       ├── story/
│       │   ├── StoryCreatedEvent.java
│       │   ├── StoryUpdatedEvent.java
│       │   ├── StoryDeletedEvent.java
│       │   └── VotingStartedEvent.java
│       └── vote/
│           ├── VoteCastEvent.java
│           ├── VoteUpdatedEvent.java
│           ├── VotesRevealedEvent.java
│           └── VotingFinishedEvent.java
└── pom.xml
```

---

## ⚙️ Kafka Serialization Configuration

Events are serialized as JSON. Configure in each service:

```yaml
spring:
  kafka:
    producer:
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.lufthansa.planning_poker.common.event
```

---

## 📦 Dependencies

| Dependency | Purpose |
|------------|---------|
| Lombok | Reduce boilerplate (getters, setters, builders) |
| Jackson | JSON serialization |

---

## 📝 Notes

- This module has no Spring Boot dependencies
- It's a pure Java library
- No tests in this module (tested via integration tests in services)
- Version is synchronized with parent POM
