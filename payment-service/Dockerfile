# ============================================================================
# Multi-stage Dockerfile for Payment Service
# ============================================================================
#
# This Dockerfile uses multi-stage build to create an optimized Docker image.
#
# Benefits of multi-stage builds:
# - Smaller final image size (only runtime dependencies included)
# - Faster deployment (less data to transfer)
# - More secure (no build tools in production image)
# - Clean separation of build and runtime environments
#
# Stages:
# 1. Build stage: Compile Java code with Maven
# 2. Runtime stage: Run compiled application with JRE
#
# ============================================================================

# ============================================================================
# Stage 1: Build Stage
# ============================================================================
# Use Maven image with JDK 17 for building
# This image includes both Maven and JDK needed for compilation
FROM maven:3.9-amazoncorretto-17 AS build

# Set working directory inside container
# All subsequent commands execute in this directory
WORKDIR /app

# Copy pom.xml first (for dependency caching)
# Docker caches layers - if pom.xml unchanged, dependencies won't re-download
# This optimization speeds up builds when only source code changes
COPY pom.xml .

# Download dependencies
# This step is cached if pom.xml hasn't changed
# Maven downloads all dependencies specified in pom.xml
RUN mvn dependency:go-offline -B

# Copy source code
# This layer is rebuilt when source code changes
COPY src ./src

# Build the application
# Creates executable JAR file in target/ directory
# -DskipTests: Skip running tests during Docker build (run tests in CI/CD instead)
# -B: Batch mode (less verbose output)
# clean: Remove old build artifacts
# package: Compile, test (skipped), and package as JAR
RUN mvn clean package -DskipTests -B

# ============================================================================
# Stage 2: Runtime Stage
# ============================================================================
# Use smaller JRE-only image for runtime
# amazoncorretto: AWS's OpenJDK distribution (free, production-ready)
# 17-alpine: JRE 17 on Alpine Linux (minimal, secure)
FROM amazoncorretto:17-alpine

# Set working directory
WORKDIR /app

# Copy JAR file from build stage
# --from=build: Copy from previous stage
# Source: /app/target/payment-service-1.0.0-SNAPSHOT.jar (from build stage)
# Destination: /app/payment-service.jar (in runtime stage)
COPY --from=build /app/target/payment-service-1.0.0-SNAPSHOT.jar payment-service.jar

# Expose port 8084
# Payment Service listens on port 8084
# This is documentation only - doesn't actually publish the port
# Use -p flag when running container to publish port
EXPOSE 8084

# Run the application
# ENTRYPOINT: Main command that runs when container starts
# Cannot be overridden (unlike CMD)
#
# java: JVM command
# -jar: Run JAR file
# payment-service.jar: Our application JAR
#
# JVM options (can be added):
# -Xms512m: Initial heap size
# -Xmx1024m: Maximum heap size
# -XX:+UseG1GC: Use G1 garbage collector
#
# Example with JVM options:
# ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-jar", "payment-service.jar"]
ENTRYPOINT ["java", "-jar", "payment-service.jar"]

# ============================================================================
# Building and Running
# ============================================================================
#
# Build image:
# docker build -t payment-service:latest .
#
# Run container:
# docker run -p 8084:8084 \
#   -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3309/paymentdb \
#   -e SPRING_DATASOURCE_USERNAME=root \
#   -e SPRING_DATASOURCE_PASSWORD=root \
#   -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ \
#   payment-service:latest
#
# Or use docker-compose (recommended):
# docker-compose up -d
#
# ============================================================================
# Image Size Comparison
# ============================================================================
#
# Single-stage (with Maven and JDK): ~800 MB
# Multi-stage (JRE only): ~200 MB
# Savings: ~600 MB (75% smaller!)
#
# ============================================================================
# Security Notes
# ============================================================================
#
# 1. Non-root user (future enhancement):
#    RUN addgroup -S spring && adduser -S spring -G spring
#    USER spring
#
# 2. Health check:
#    HEALTHCHECK --interval=30s --timeout=3s \
#      CMD wget -q --spider http://localhost:8084/actuator/health || exit 1
#
# 3. Secrets management:
#    Don't hardcode passwords in Dockerfile
#    Use environment variables or Docker secrets
#
# ============================================================================
