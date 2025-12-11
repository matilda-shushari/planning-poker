# 🚪 API Gateway

**Planning Poker Platform - Single Entry Point & Request Routing**

---

## 🚀 Quick Start

### Access Points

| Resource | URL |
|----------|-----|
| **Gateway (Main Entry)** | http://localhost:8080 |
| **Health Check** | http://localhost:8080/actuator/health |
| **Prometheus Metrics** | http://localhost:8080/actuator/prometheus |

### Running the Service

**Option 1: Using Maven (Development)**
```bash
# From project root directory
cd planning-poker
.\mvnw spring-boot:run -pl pp-gateway
```

**Option 2: Using Docker Compose (Full Stack)**
```bash
# Starts all services including Gateway
docker compose up -d
```

**Option 3: Running JAR directly**
```bash
# Build first
.\mvnw package -pl pp-gateway -DskipTests

# Run
java -jar pp-gateway/target/pp-gateway-0.0.1-SNAPSHOT.jar
```

### Prerequisites

Before running, ensure these are available:
- ✅ Keycloak (port 8180) with `planning-poker` realm
- ✅ Room Service (port 8081)
- ✅ Vote Service (port 8082)
- ✅ Audit Service (port 8083)

**Start everything:**
```bash
docker compose up -d
```

### Verify Service is Running

```bash
# Check health
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}
```

---

## 📖 Overview

The API Gateway serves as the single entry point for all client requests. It handles JWT token validation, request routing to appropriate microservices, and provides a unified API surface for all clients (web, mobile, etc.).

**Default Port**: `8080`

## Responsibilities

| Feature | Description |
|---------|-------------|
| **Request Routing** | Route requests to correct microservice |
| **JWT Validation** | Validate tokens against Keycloak |
| **Single Entry Point** | Unified API for all clients |
| **Security Gateway** | Centralized authentication |
| **Load Balancing** | Distribute requests (when scaled) |

---

## 🔀 Routing Configuration

| Path Pattern | Target Service | Port | Description |
|--------------|----------------|------|-------------|
| `/api/v1/rooms/**` | Room Service | 8081 | Room management |
| `/api/v1/stories/**` | Room Service | 8081 | Story management |
| `/api/v1/votes/**` | Vote Service | 8082 | Vote casting |
| `/api/v1/voting/**` | Vote Service | 8082 | Voting control |
| `/api/v1/admin/audit/**` | Audit Service | 8083 | Audit logs |
| `/api/v1/admin/rooms/**` | Room Service | 8081 | Admin room management |
| `/api/v1/admin/stories/**` | Room Service | 8081 | Admin story management |

---

## 🏗️ Architecture Position

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENTS                                 │
│              (Web Browser / Mobile App / Postman)               │
└─────────────────────────────┬───────────────────────────────────┘
                              │
                              │ All requests go through Gateway
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        API GATEWAY                              │
│                        (Port 8080)                              │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                  Security Layer                          │   │
│  │  • JWT Token Validation                                  │   │
│  │  • Keycloak Integration                                  │   │
│  │  • Role Extraction                                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                  Routing Layer                           │   │
│  │  • Path-based routing                                    │   │
│  │  • Service discovery                                     │   │
│  │  • Load balancing                                        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────┬───────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│ Room Service  │    │ Vote Service  │    │ Audit Service │
│   (8081)      │    │   (8082)      │    │   (8083)      │
└───────────────┘    └───────────────┘    └───────────────┘
```

---

## 🔐 Security Configuration

### JWT Validation
- All requests (except health checks) require valid JWT
- Token is validated against Keycloak's public keys
- User info and roles are extracted from token

### Public Endpoints (No Auth Required)
- `/actuator/health` - Health check
- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe

---

## 🔄 Request Flow

```
1. Client sends request to Gateway (port 8080)
          │
          ▼
2. Gateway validates JWT token
   ├── Invalid token → 401 Unauthorized
   └── Valid token → Continue
          │
          ▼
3. Gateway matches route based on path
   ├── /api/v1/rooms/** → Room Service
   ├── /api/v1/votes/** → Vote Service
   └── /api/v1/admin/audit/** → Audit Service
          │
          ▼
4. Gateway forwards request to target service
          │
          ▼
5. Target service processes request
          │
          ▼
6. Response flows back through Gateway to client
```

---

## ⚙️ Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `KEYCLOAK_HOST` | Keycloak hostname | `keycloak` |
| `ROOM_SERVICE_HOST` | Room Service hostname | `pp-room-service` |
| `ROOM_SERVICE_PORT` | Room Service port | `8081` |
| `VOTE_SERVICE_HOST` | Vote Service hostname | `pp-vote-service` |
| `VOTE_SERVICE_PORT` | Vote Service port | `8082` |
| `AUDIT_SERVICE_HOST` | Audit Service hostname | `pp-audit-service` |
| `AUDIT_SERVICE_PORT` | Audit Service port | `8083` |

---

## 📁 Project Structure

```
pp-gateway/
├── src/main/java/com/lufthansa/planning_poker/gateway/
│   ├── GatewayApplication.java     # Main application class
│   └── config/
│       └── SecurityConfig.java     # OAuth2 & routing config
├── src/main/resources/
│   └── application.yml             # Configuration
└── Dockerfile                      # Container build
```

---

## 🔧 Troubleshooting

### Common Issues

**1. 401 Unauthorized on all requests**
- Check Keycloak is running
- Verify Keycloak realm exists
- Ensure token is not expired

**2. 503 Service Unavailable**
- Check target service is running
- Verify service host/port configuration
- Check network connectivity in Docker

**3. Gateway not starting**
- Verify Keycloak issuer-uri is accessible
- Check port 8080 is not in use

---

## 📦 Dependencies

| Dependency | Purpose |
|------------|---------|
| Spring Boot 3.4.x | Framework |
| Spring Cloud Gateway | API Gateway functionality |
| Spring Security | OAuth2 Resource Server |
| Spring Boot Actuator | Health checks & metrics |
