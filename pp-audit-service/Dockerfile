# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY pp-common/pom.xml pp-common/
COPY pp-audit-service/pom.xml pp-audit-service/

RUN ./mvnw dependency:go-offline -pl pp-audit-service -am

COPY pp-common/src pp-common/src
COPY pp-audit-service/src pp-audit-service/src

RUN ./mvnw package -pl pp-audit-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

COPY --from=builder /app/pp-audit-service/target/*.jar app.jar

USER appuser

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

