# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY pp-common/pom.xml pp-common/
COPY pp-vote-service/pom.xml pp-vote-service/

RUN ./mvnw dependency:go-offline -pl pp-vote-service -am

COPY pp-common/src pp-common/src
COPY pp-vote-service/src pp-vote-service/src

RUN ./mvnw package -pl pp-vote-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

COPY --from=builder /app/pp-vote-service/target/*.jar app.jar

USER appuser

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

