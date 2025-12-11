# 🏠 Room Service

**Planning Poker Platform - Room & Story Management Service**

---

## 🚀 Quick Start

### Access Points

| Resource | URL |
|----------|-----|
| **Swagger UI** | http://localhost:8081/swagger-ui.html |
| **Health Check** | http://localhost:8081/actuator/health |
| **OpenAPI JSON** | http://localhost:8081/v3/api-docs |
| **Prometheus Metrics** | http://localhost:8081/actuator/prometheus |

### Running the Service

**Option 1: Using Maven (Development)**
```bash
# From project root directory
cd planning-poker
.\mvnw spring-boot:run -pl pp-room-service
```

**Option 2: Using Docker Compose (Full Stack)**
```bash
# Starts all services including Room Service
docker compose up -d
```

**Option 3: Running JAR directly**
```bash
# Build first
.\mvnw package -pl pp-room-service -DskipTests

# Run
java -jar pp-room-service/target/pp-room-service-0.0.1-SNAPSHOT.jar
```

### Prerequisites

Before running, ensure these are available:
- ✅ PostgreSQL (port 5432) with database `planning_poker_rooms`
- ✅ Redis (port 6379)
- ✅ Kafka (port 9092)
- ✅ Keycloak (port 8180) with `planning-poker` realm

**Start infrastructure only:**
```bash
docker compose up -d postgres redis kafka zookeeper keycloak
```

### Verify Service is Running

```bash
# Check health
curl http://localhost:8081/actuator/health

# Expected response:
# {"status":"UP"}
```

---

## 📖 Overview

The Room Service is the core service responsible for managing planning poker rooms, stories, participants, and invitations. It serves as the primary entry point for moderators to create and manage estimation sessions.

**Default Port**: `8081`

## Responsibilities

| Feature | Description |
|---------|-------------|
| **Room Management** | Create, read, update, delete planning poker rooms |
| **Story Management** | Manage stories/tasks within rooms for estimation |
| **Participant Management** | Track who joins rooms and their roles |
| **Short Link Generation** | Generate shareable codes for room invites (e.g., "ABC123") |
| **Email Invitations** | Send invitation links to participants |
| **Deck Types** | Support multiple estimation deck types |

---

## 🎴 Supported Deck Types

| Type | Values |
|------|--------|
| `FIBONACCI` | 0, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, ? |
| `SCRUM` | 0, 0.5, 1, 2, 3, 5, 8, 13, 20, 40, 100, ?, ☕ |
| `SEQUENTIAL` | 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, ? |
| `TSHIRT` | XS, S, M, L, XL, XXL, ? |
| `CUSTOM` | User-defined values |

---

## 📡 API Endpoints

### Room Endpoints

| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| `POST` | `/api/v1/rooms` | Create a new room | USER |
| `GET` | `/api/v1/rooms/{id}` | Get room by ID | USER |
| `GET` | `/api/v1/rooms/code/{shortCode}` | Get room by short code | USER |
| `GET` | `/api/v1/rooms/my-rooms` | Get user's moderated rooms | USER |
| `PUT` | `/api/v1/rooms/{id}` | Update room | MODERATOR |
| `DELETE` | `/api/v1/rooms/{id}` | Delete room | MODERATOR/ADMIN |
| `POST` | `/api/v1/rooms/join/{shortCode}` | Join a room | USER |

### Story Endpoints

| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| `POST` | `/api/v1/rooms/{roomId}/stories` | Create a story | MODERATOR |
| `GET` | `/api/v1/rooms/{roomId}/stories` | Get all stories in room | USER |
| `GET` | `/api/v1/stories/{id}` | Get story by ID | USER |
| `PUT` | `/api/v1/stories/{id}` | Update story | MODERATOR |
| `DELETE` | `/api/v1/stories/{id}` | Delete story | MODERATOR/ADMIN |
| `POST` | `/api/v1/stories/{id}/start-voting` | Start voting on story | MODERATOR |

### Admin Endpoints

| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| `GET` | `/api/v1/admin/rooms` | Get all rooms | ADMIN |
| `DELETE` | `/api/v1/admin/rooms/{id}` | Force delete any room | ADMIN |
| `GET` | `/api/v1/admin/stories` | Get all stories | ADMIN |

### Invitation Endpoints

| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| `POST` | `/api/v1/rooms/{roomId}/invite` | Send email invitation | MODERATOR |

---

## 🗄️ Database

- **Database Name**: `planning_poker_rooms`
- **Migration Tool**: Liquibase (automatic on startup)

### Tables

| Table | Description |
|-------|-------------|
| `rooms` | Room information (name, deck type, moderator, etc.) |
| `stories` | Stories within rooms for estimation |
| `room_participants` | Room membership and roles |
| `invitations` | Email invitation records |

---

## 📨 Kafka Events Published

| Event | Topic | When |
|-------|-------|------|
| `RoomCreatedEvent` | `room-events` | Room created |
| `RoomUpdatedEvent` | `room-events` | Room updated |
| `RoomDeletedEvent` | `room-events` | Room deleted |
| `StoryCreatedEvent` | `story-events` | Story created |
| `StoryUpdatedEvent` | `story-events` | Story updated |
| `StoryDeletedEvent` | `story-events` | Story deleted |
| `VotingStartedEvent` | `story-events` | Voting started on story |

---

## ⚙️ Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_NAME` | Database name | `planning_poker_rooms` |
| `DB_USER` | Database user | `planning_poker` |
| `DB_PASSWORD` | Database password | `planning_poker` |
| `REDIS_HOST` | Redis host for caching | `localhost` |
| `KAFKA_SERVERS` | Kafka bootstrap servers | `localhost:9092` |
| `KEYCLOAK_HOST` | Keycloak host | `localhost` |

---

## 🧪 Running Tests

```bash
# Run all tests
.\mvnw test -pl pp-room-service

# Run with coverage report
.\mvnw test jacoco:report -pl pp-room-service

# View coverage report
start pp-room-service/target/site/jacoco/index.html
```

---

## 📁 Project Structure

```
pp-room-service/
├── src/main/java/com/lufthansa/planning_poker/room/
│   ├── api/
│   │   ├── controller/        # REST controllers
│   │   └── exception/         # Exception handlers
│   ├── application/
│   │   ├── dto/               # Request/Response DTOs
│   │   ├── mapper/            # Entity ↔ DTO mappers
│   │   └── usecase/           # Business logic
│   ├── domain/
│   │   └── model/             # Domain entities & enums
│   └── infrastructure/
│       ├── config/            # Spring configurations
│       ├── messaging/         # Kafka producers
│       └── persistence/       # JPA entities & repositories
├── src/main/resources/
│   ├── application.yml
│   └── db/changelog/          # Liquibase migrations
└── src/test/java/             # Unit tests (69+ tests)
```

---

## 📦 Dependencies

| Dependency | Purpose |
|------------|---------|
| Spring Boot 3.4.x | Framework |
| Spring Security | OAuth2 Resource Server |
| Spring Data JPA | Database access |
| PostgreSQL | Primary database |
| Redis | Caching |
| Kafka | Event publishing |
| Liquibase | Database migrations |
| MapStruct | DTO mapping |
| SpringDoc OpenAPI | API documentation |
