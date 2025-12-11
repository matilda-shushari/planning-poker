# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Copy ALL module pom.xml files (required for parent pom resolution)
COPY pp-common/pom.xml pp-common/
COPY pp-gateway/pom.xml pp-gateway/
COPY pp-room-service/pom.xml pp-room-service/
COPY pp-vote-service/pom.xml pp-vote-service/
COPY pp-audit-service/pom.xml pp-audit-service/

RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -pl pp-gateway -am -B

COPY pp-common/src pp-common/src
COPY pp-gateway/src pp-gateway/src

RUN ./mvnw package -pl pp-gateway -am -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

COPY --from=builder /app/pp-gateway/target/*.jar app.jar

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
