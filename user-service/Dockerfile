# Multi-stage Dockerfile for User Service

# Stage 1: Build
FROM maven:3.9.5-amazoncorretto-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM amazoncorretto:17-alpine
WORKDIR /app
COPY --from=builder /app/target/user-service-1.0.0.jar user-service.jar

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
RUN chown -R spring:spring /app
USER spring

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8082/user-service/actuator/health || exit 1

ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-XX:+UseG1GC", "-jar", "user-service.jar"]
