# 📋 Audit Service

**Planning Poker Platform - Event Logging & Audit Trail Service**

---

## 🚀 Quick Start

### Access Points

| Resource | URL |
|----------|-----|
| **Swagger UI** | http://localhost:8083/swagger-ui.html |
| **Health Check** | http://localhost:8083/actuator/health |
| **OpenAPI JSON** | http://localhost:8083/v3/api-docs |
| **Prometheus Metrics** | http://localhost:8083/actuator/prometheus |

⚠️ **Note**: All API endpoints require **ADMIN** role!

### Running the Service

**Option 1: Using Maven (Development)**
```bash
# From project root directory
cd planning-poker
.\mvnw spring-boot:run -pl pp-audit-service
```

**Option 2: Using Docker Compose (Full Stack)**
```bash
# Starts all services including Audit Service
docker compose up -d
```

**Option 3: Running JAR directly**
```bash
# Build first
.\mvnw package -pl pp-audit-service -DskipTests

# Run
java -jar pp-audit-service/target/pp-audit-service-0.0.1-SNAPSHOT.jar
```

### Prerequisites

Before running, ensure these are available:
- ✅ PostgreSQL (port 5432) with database `planning_poker_audit`
- ✅ Kafka (port 9092)
- ✅ Keycloak (port 8180) with `planning-poker` realm

**Start infrastructure only:**
```bash
docker compose up -d postgres kafka zookeeper keycloak
```

### Verify Service is Running

```bash
# Check health
curl http://localhost:8083/actuator/health

# Expected response:
# {"status":"UP"}
```

---

## 📖 Overview

The Audit Service consumes events from Kafka and maintains a complete audit trail of all platform activities. It provides admin-only endpoints to query and analyze system events for compliance, debugging, and monitoring purposes.

**Default Port**: `8083`

## Responsibilities

| Feature | Description |
|---------|-------------|
| **Event Consumption** | Listen to all Kafka topics for system events |
| **Audit Logging** | Store events with timestamps, user info, and details |
| **Admin Queries** | Provide filtered access to audit data |
| **Compliance** | Maintain immutable audit trail for regulatory needs |
| **Debugging** | Track system activities for troubleshooting |

---

## 📡 API Endpoints (Admin Only)

⚠️ **All endpoints require ADMIN role**

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/admin/audit` | Get all audit logs (paginated) |
| `GET` | `/api/v1/admin/audit/{id}` | Get specific audit entry |
| `GET` | `/api/v1/admin/audit/entity/{type}/{id}` | Filter by entity type and ID |
| `GET` | `/api/v1/admin/audit/user/{userId}` | Filter by user ID |
| `GET` | `/api/v1/admin/audit/action/{action}` | Filter by action type |
| `GET` | `/api/v1/admin/audit/search` | Advanced search with multiple filters |

### Query Parameters for Pagination

| Parameter | Description | Default |
|-----------|-------------|---------|
| `page` | Page number (0-based) | 0 |
| `size` | Items per page | 20 |
| `sort` | Sort field and direction | `timestamp,desc` |

---

## 📨 Kafka Events Consumed

### Room Events (Topic: `room-events`)

| Event | Description |
|-------|-------------|
| `RoomCreatedEvent` | New room created |
| `RoomUpdatedEvent` | Room details modified |
| `RoomDeletedEvent` | Room deleted |
| `UserJoinedEvent` | User joined a room |
| `UserLeftEvent` | User left a room |

### Story Events (Topic: `story-events`)

| Event | Description |
|-------|-------------|
| `StoryCreatedEvent` | New story added |
| `StoryUpdatedEvent` | Story modified |
| `StoryDeletedEvent` | Story removed |
| `VotingStartedEvent` | Voting opened on story |

### Vote Events (Topic: `vote-events`)

| Event | Description |
|-------|-------------|
| `VoteCastEvent` | Vote submitted |
| `VoteUpdatedEvent` | Vote changed |
| `VotesRevealedEvent` | Votes revealed to room |
| `VotingFinishedEvent` | Voting completed |

---

## 📄 Audit Log Entry Structure

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "action": "ROOM_CREATED",
  "entityType": "ROOM",
  "entityId": "room-uuid-here",
  "userId": "user-uuid-from-keycloak",
  "userDisplayName": "matilda",
  "details": "Room 'Sprint 47 - Lufthansa Demo' created",
  "timestamp": "2025-12-09T12:00:00Z",
  "metadata": {
    "roomName": "Sprint 47 - Lufthansa Demo",
    "deckType": "FIBONACCI",
    "moderatorId": "user-uuid"
  }
}
```

---

## 📊 Available Actions (Audit Log Types)

| Action | Entity Type | Description |
|--------|-------------|-------------|
| `ROOM_CREATED` | ROOM | New room created |
| `ROOM_UPDATED` | ROOM | Room modified |
| `ROOM_DELETED` | ROOM | Room deleted |
| `USER_JOINED` | ROOM | User joined room |
| `USER_LEFT` | ROOM | User left room |
| `STORY_CREATED` | STORY | Story added |
| `STORY_UPDATED` | STORY | Story modified |
| `STORY_DELETED` | STORY | Story removed |
| `VOTING_STARTED` | STORY | Voting opened |
| `VOTE_CAST` | VOTE | Vote submitted |
| `VOTE_UPDATED` | VOTE | Vote changed |
| `VOTES_REVEALED` | STORY | Votes revealed |
| `VOTING_FINISHED` | STORY | Voting completed |

---

## 🗄️ Database

- **Database Name**: `planning_poker_audit`
- **Migration Tool**: Liquibase (automatic on startup)

### Tables

| Table | Description |
|-------|-------------|
| `audit_logs` | All audit entries |

---

## ⚙️ Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_NAME` | Database name | `planning_poker_audit` |
| `DB_USER` | Database user | `planning_poker` |
| `DB_PASSWORD` | Database password | `planning_poker` |
| `KAFKA_SERVERS` | Kafka bootstrap servers | `localhost:9092` |
| `KEYCLOAK_HOST` | Keycloak host | `localhost` |

---

## 🧪 Running Tests

```bash
# Run all tests
.\mvnw test -pl pp-audit-service

# Run with coverage report
.\mvnw test jacoco:report -pl pp-audit-service

# View coverage report
start pp-audit-service/target/site/jacoco/index.html
```

---

## 📁 Project Structure

```
pp-audit-service/
├── src/main/java/com/lufthansa/planning_poker/audit/
│   ├── api/
│   │   └── controller/        # Admin REST controllers
│   ├── application/
│   │   ├── dto/               # Response DTOs
│   │   └── usecase/           # Query logic
│   ├── domain/
│   │   └── model/             # Domain entities
│   └── infrastructure/
│       ├── config/            # Spring & Security config
│       ├── messaging/         # Kafka consumers
│       └── persistence/       # JPA entities & repositories
├── src/main/resources/
│   ├── application.yml
│   └── db/changelog/          # Liquibase migrations
└── src/test/java/             # Unit tests
```

---

## 🔧 Troubleshooting

### Common Issues

**1. Empty pageable field in Swagger causes 500 error**
- Solution: Clear the pageable field or use `{}`

**2. No audit logs appearing**
- Check Kafka is running: `docker compose ps`
- Check consumer group: `docker exec pp-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list`
- Verify other services are publishing events

**3. 403 Forbidden**
- Ensure you're using a token with ADMIN role
- Check Keycloak role mapper is configured

---

## 📦 Dependencies

| Dependency | Purpose |
|------------|---------|
| Spring Boot 3.4.x | Framework |
| Spring Security | OAuth2 Resource Server |
| Spring Kafka | Event consumption |
| Spring Data JPA | Database access |
| PostgreSQL | Primary database |
| Liquibase | Database migrations |
| SpringDoc OpenAPI | API documentation |
