# 🗳️ Vote Service

**Planning Poker Platform - Voting & Real-time Updates Service**

---

## 🚀 Quick Start

### Access Points

| Resource | URL |
|----------|-----|
| **Swagger UI** | http://localhost:8082/swagger-ui.html |
| **Health Check** | http://localhost:8082/actuator/health |
| **WebSocket** | ws://localhost:8082/ws |
| **OpenAPI JSON** | http://localhost:8082/v3/api-docs |
| **Prometheus Metrics** | http://localhost:8082/actuator/prometheus |

### Running the Service

**Option 1: Using Maven (Development)**
```bash
# From project root directory
cd planning-poker
.\mvnw spring-boot:run -pl pp-vote-service
```

**Option 2: Using Docker Compose (Full Stack)**
```bash
# Starts all services including Vote Service
docker compose up -d
```

**Option 3: Running JAR directly**
```bash
# Build first
.\mvnw package -pl pp-vote-service -DskipTests

# Run
java -jar pp-vote-service/target/pp-vote-service-0.0.1-SNAPSHOT.jar
```

### Prerequisites

Before running, ensure these are available:
- ✅ PostgreSQL (port 5432) with database `planning_poker_votes`
- ✅ Kafka (port 9092)
- ✅ Keycloak (port 8180) with `planning-poker` realm

**Start infrastructure only:**
```bash
docker compose up -d postgres kafka zookeeper keycloak
```

### Verify Service is Running

```bash
# Check health
curl http://localhost:8082/actuator/health

# Expected response:
# {"status":"UP"}
```

---

## 📖 Overview

The Vote Service handles all voting functionality including casting votes, revealing results, calculating averages, and broadcasting updates via WebSocket for real-time participant synchronization.

**Default Port**: `8082`

## Responsibilities

| Feature | Description |
|---------|-------------|
| **Vote Casting** | Participants submit estimation votes |
| **Vote Management** | Update or remove votes before reveal |
| **Vote Counting** | Track number of votes per story |
| **Results Calculation** | Calculate average score and statistics |
| **Consensus Detection** | Detect when all participants agree |
| **Vote Reveal** | Show all votes to participants |
| **Finish Voting** | Lock voting and record final estimate |
| **WebSocket Broadcast** | Real-time updates to all room participants |

---

## 📡 API Endpoints

### Voting Endpoints

| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| `POST` | `/api/v1/votes` | Cast a vote | USER |
| `PUT` | `/api/v1/votes/{id}` | Update existing vote | USER |
| `DELETE` | `/api/v1/votes/{id}` | Remove vote | USER |
| `GET` | `/api/v1/votes/stories/{storyId}` | Get all votes for story | USER |
| `GET` | `/api/v1/votes/stories/{storyId}/count` | Get vote count | USER |
| `GET` | `/api/v1/votes/stories/{storyId}/my-vote` | Get current user's vote | USER |

### Voting Control Endpoints (Moderator Only)

| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| `POST` | `/api/v1/voting/stories/{storyId}/reveal` | Reveal all votes | MODERATOR |
| `POST` | `/api/v1/voting/stories/{storyId}/finish` | Finish voting session | MODERATOR |
| `POST` | `/api/v1/voting/stories/{storyId}/reset` | Reset all votes | MODERATOR |

---

## 🔌 WebSocket Integration

### Connection

Connect to WebSocket at: `ws://localhost:8082/ws`

### Topics

| Topic | Direction | Purpose |
|-------|-----------|---------|
| `/topic/room/{roomId}/votes` | Server → Client | Vote count updates |
| `/topic/room/{roomId}/results` | Server → Client | Vote reveal results |
| `/topic/room/{roomId}/finished` | Server → Client | Voting finished notification |

### Message Formats

**Vote Count Update:**
```json
{
  "storyId": "uuid",
  "voteCount": 5,
  "totalParticipants": 7
}
```

**Vote Results (on reveal):**
```json
{
  "storyId": "uuid",
  "votes": [
    {"userDisplayName": "matilda", "value": "5"},
    {"userDisplayName": "florenc", "value": "8"},
    {"userDisplayName": "admin", "value": "5"}
  ],
  "average": 6.0,
  "hasConsensus": false,
  "voteCount": 3
}
```

**Voting Finished:**
```json
{
  "storyId": "uuid",
  "storyTitle": "LH-101: Implement seat selection",
  "finalEstimate": "8",
  "average": 6.5,
  "voteCount": 3
}
```

---

## 🔄 Real-time Voting Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    VOTING FLOW                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Moderator starts voting (Room Service)                      │
│                          ↓                                      │
│  2. Participants cast votes (POST /api/v1/votes)                │
│                          ↓                                      │
│  3. WebSocket broadcasts vote count to all                      │
│     (/topic/room/{roomId}/votes)                                │
│                          ↓                                      │
│  4. Moderator reveals votes (POST .../reveal)                   │
│                          ↓                                      │
│  5. WebSocket broadcasts results to all                         │
│     (/topic/room/{roomId}/results)                              │
│                          ↓                                      │
│  6. Moderator finishes voting (POST .../finish)                 │
│                          ↓                                      │
│  7. WebSocket broadcasts final estimate                         │
│     (/topic/room/{roomId}/finished)                             │
│                          ↓                                      │
│  8. Kafka publishes VotingFinishedEvent                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🗄️ Database

- **Database Name**: `planning_poker_votes`
- **Migration Tool**: Liquibase (automatic on startup)

### Tables

| Table | Description |
|-------|-------------|
| `votes` | Individual vote records |

---

## 📨 Kafka Events Published

| Event | Topic | When |
|-------|-------|------|
| `VoteCastEvent` | `vote-events` | Vote submitted |
| `VoteUpdatedEvent` | `vote-events` | Vote changed |
| `VotesRevealedEvent` | `vote-events` | All votes revealed |
| `VotingFinishedEvent` | `vote-events` | Voting completed with final estimate |

---

## ⚙️ Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_NAME` | Database name | `planning_poker_votes` |
| `DB_USER` | Database user | `planning_poker` |
| `DB_PASSWORD` | Database password | `planning_poker` |
| `KAFKA_SERVERS` | Kafka bootstrap servers | `localhost:9092` |
| `KEYCLOAK_HOST` | Keycloak host | `localhost` |

---

## 🧪 Running Tests

```bash
# Run all tests
.\mvnw test -pl pp-vote-service

# Run with coverage report
.\mvnw test jacoco:report -pl pp-vote-service

# View coverage report
start pp-vote-service/target/site/jacoco/index.html
```

---

## 📁 Project Structure

```
pp-vote-service/
├── src/main/java/com/lufthansa/planning_poker/vote/
│   ├── api/
│   │   ├── controller/        # REST controllers
│   │   └── websocket/         # WebSocket handlers
│   ├── application/
│   │   ├── dto/               # Request/Response DTOs
│   │   └── usecase/           # Business logic
│   ├── domain/
│   │   └── model/             # Domain entities
│   └── infrastructure/
│       ├── config/            # Spring & WebSocket config
│       ├── messaging/         # Kafka producers
│       └── persistence/       # JPA entities & repositories
├── src/main/resources/
│   ├── application.yml
│   └── db/changelog/          # Liquibase migrations
└── src/test/java/             # Unit tests
```

---

## 📦 Dependencies

| Dependency | Purpose |
|------------|---------|
| Spring Boot 3.4.x | Framework |
| Spring Security | OAuth2 Resource Server |
| Spring WebSocket | Real-time communication |
| Spring Data JPA | Database access |
| PostgreSQL | Primary database |
| Kafka | Event publishing |
| Liquibase | Database migrations |
| SpringDoc OpenAPI | API documentation |
