# =============================================================================
# Inventory Service - Multi-Stage Dockerfile
# =============================================================================
#
# Purpose:
# --------
# Creates optimized Docker image for Inventory Service microservice.
# Uses multi-stage build to minimize final image size.
#
# Build Stages:
# -------------
# 1. Build stage: Compile Java code with Maven
# 2. Runtime stage: Run compiled JAR with JRE only
#
# Image Sizes:
# ------------
# - Build stage: ~800MB (Maven + JDK + dependencies)
# - Final image: ~250MB (JRE + JAR only)
#
# Build Command:
# --------------
# docker build -t inventory-service:latest .
#
# Run Command:
# ------------
# docker run -p 8085:8085 \
#   -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/inventorydb \
#   -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/ \
#   inventory-service:latest
#
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build
# -----------------------------------------------------------------------------
# Use Maven image with JDK 17 for building the application.
# This stage compiles Java code and packages it into an executable JAR.
#
# Why eclipse-temurin?
# - Official OpenJDK distribution
# - Long-term support (LTS)
# - Production-ready
# - Well-maintained
#
FROM eclipse-temurin:17-jdk-alpine AS build

# Set working directory for build
# All subsequent commands run in this directory.
#
WORKDIR /app

# Copy Maven configuration files first
# Why copy pom.xml separately?
# - Docker layer caching optimization
# - If pom.xml unchanged, dependencies layer is cached
# - Faster rebuilds when only code changes
#
# Files copied:
# - pom.xml: Maven project configuration
# - .mvn/: Maven wrapper configuration (if exists)
#
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Download dependencies
# This step is cached if pom.xml hasn't changed.
# Saves time on rebuilds.
#
# -B: Batch mode (non-interactive)
# -e: Show error stack traces
# dependency:go-offline: Download all dependencies
#
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B -e

# Copy source code
# Now that dependencies are cached, copy application code.
#
COPY src src

# Build the application
# Compile Java code and package into JAR file.
#
# Maven commands:
# - clean: Remove previous build artifacts
# - package: Compile, test, and package into JAR
# - -DskipTests: Skip running tests (run tests in CI/CD instead)
#
# Output:
# - JAR file created at: target/inventory-service-0.0.1-SNAPSHOT.jar
#
RUN ./mvnw clean package -DskipTests

# -----------------------------------------------------------------------------
# Stage 2: Runtime
# -----------------------------------------------------------------------------
# Use smaller JRE image for running the application.
# No need for JDK or Maven in production.
#
# Image size comparison:
# - JDK image: ~450MB
# - JRE image: ~180MB
# - Savings: ~270MB (60% reduction)
#
FROM eclipse-temurin:17-jre-alpine

# Set working directory for runtime
#
WORKDIR /app

# Copy JAR file from build stage
# Only copy the compiled JAR, not source code or build tools.
#
# Syntax: COPY --from=<stage> <source> <destination>
# - --from=build: Copy from "build" stage
# - Source: /app/target/*.jar (the compiled JAR)
# - Destination: /app/inventory-service.jar (renamed for simplicity)
#
COPY --from=build /app/target/*.jar inventory-service.jar

# Expose port 8085
# This is the port Inventory Service listens on.
#
# Note:
# - This is documentation only
# - Doesn't actually publish the port
# - Use -p 8085:8085 when running container
#
EXPOSE 8085

# Set JVM options for containerized environment
# Optimize Java for running in Docker/Kubernetes.
#
# Environment variable: JAVA_OPTS
# Options explained:
# -XX:+UseContainerSupport
#   - JVM detects container memory limits
#   - Automatically adjusts heap size
#   - Prevents OOM (Out of Memory) errors
#
# -XX:MaxRAMPercentage=75.0
#   - Use up to 75% of container memory for heap
#   - Leave 25% for non-heap (metaspace, threads, etc.)
#   - Example: 1GB container = 750MB heap
#
# -Djava.security.egd=file:/dev/./urandom
#   - Use non-blocking random number generator
#   - Faster startup time
#   - Important for JWT token generation
#
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Run the application
# Start the Spring Boot application using java -jar.
#
# Command breakdown:
# - sh -c: Run command in shell (allows variable expansion)
# - $JAVA_OPTS: JVM options from ENV variable
# - -jar: Run JAR file
# - inventory-service.jar: The application JAR
#
# Why ENTRYPOINT instead of CMD?
# - ENTRYPOINT: Always executed, cannot be overridden easily
# - CMD: Can be overridden with docker run arguments
# - For microservices, ENTRYPOINT is more appropriate
#
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar inventory-service.jar"]

# =============================================================================
# Docker Image Best Practices Applied:
# =============================================================================
#
# 1. Multi-stage build
#    - Smaller final image
#    - Faster deployment
#    - Lower storage costs
#
# 2. Layer caching optimization
#    - Copy pom.xml before source code
#    - Dependencies cached separately
#    - Faster rebuilds
#
# 3. Minimal base image
#    - Alpine Linux (5MB base)
#    - Only JRE, no JDK
#    - Reduced attack surface
#
# 4. Container-aware JVM
#    - Respects memory limits
#    - Prevents OOM kills
#    - Better resource utilization
#
# 5. Non-root user (optional enhancement)
#    - Add: RUN addgroup -S spring && adduser -S spring -G spring
#    - Add: USER spring
#    - Security best practice
#
# 6. Health check (optional enhancement)
#    - Add: HEALTHCHECK --interval=30s --timeout=3s \
#           CMD wget -qO- http://localhost:8085/actuator/health || exit 1
#    - Kubernetes/Docker can monitor service health
#
# =============================================================================
# Environment Variables (Runtime Configuration):
# =============================================================================
#
# Required variables (set via docker run -e or docker-compose):
#
# Database Configuration:
# -----------------------
# SPRING_DATASOURCE_URL
#   - JDBC URL for MySQL database
#   - Example: jdbc:mysql://mysql-inventory:3306/inventorydb
#   - Default (application.yml): jdbc:mysql://localhost:3310/inventorydb
#
# SPRING_DATASOURCE_USERNAME
#   - Database username
#   - Example: inventory_user
#   - Default: root
#
# SPRING_DATASOURCE_PASSWORD
#   - Database password
#   - Example: secure_password_123
#   - Default: root
#
# Service Discovery:
# ------------------
# EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
#   - Eureka server URL
#   - Example: http://eureka-server:8761/eureka/
#   - Default: http://localhost:8761/eureka/
#
# JWT Configuration:
# ------------------
# APP_JWT_SECRET
#   - Secret key for JWT token validation
#   - Must match other services
#   - Example: mySecretKey12345
#   - Default: mySecretKey12345
#
# Inventory Configuration:
# ------------------------
# APP_INVENTORY_LOW_STOCK_THRESHOLD
#   - Default threshold for low stock alerts
#   - Example: 10
#   - Default: 10
#
# APP_INVENTORY_ALLOW_NEGATIVE_STOCK
#   - Allow negative stock (for backorders)
#   - Example: false
#   - Default: false
#
# =============================================================================
# Example docker-compose.yml Usage:
# =============================================================================
#
# services:
#   inventory-service:
#     build: ./inventory-service
#     ports:
#       - "8085:8085"
#     environment:
#       - SPRING_DATASOURCE_URL=jdbc:mysql://mysql-inventory:3306/inventorydb
#       - SPRING_DATASOURCE_USERNAME=inventory_user
#       - SPRING_DATASOURCE_PASSWORD=secure_password
#       - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
#     depends_on:
#       - mysql-inventory
#       - eureka-server
#     networks:
#       - ecommerce-network
#
# =============================================================================
