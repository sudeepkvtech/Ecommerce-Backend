# Dockerfile for Product Service
# This file defines how to build a Docker image for the Product Service

# What is a Dockerfile?
# ---------------------
# A Dockerfile is a text file containing instructions to build a Docker image
# Each instruction creates a layer in the image
# Layers are cached, making subsequent builds faster

# What is a Docker Image?
# ------------------------
# A Docker image is a lightweight, standalone, executable package that includes:
# - Application code
# - Runtime (Java)
# - Dependencies (Spring Boot, MySQL connector, etc.)
# - Configuration files
# Think of it as a "template" for creating containers

# What is a Docker Container?
# ----------------------------
# A container is a running instance of an image
# Like how an "object" is an instance of a "class" in OOP
# Multiple containers can run from the same image

# ============================================================================
# Multi-stage Build
# ============================================================================
# We use a multi-stage build to:
# 1. Build the application in one stage (build stage)
# 2. Run the application in another stage (runtime stage)
#
# Benefits:
# - Smaller final image (excludes build tools like Maven)
# - Faster deployment (less to download)
# - More secure (fewer tools = smaller attack surface)
# - Cleaner separation of build and runtime concerns

# ============================================================================
# STAGE 1: Build Stage
# ============================================================================
# This stage compiles the Java code and packages it into a JAR file

# FROM specifies the base image
# maven:3.9.5-amazoncorretto-17 provides:
# - Maven 3.9.5 (build tool)
# - Amazon Corretto 17 (Java 17 runtime)
# - Alpine Linux (lightweight OS)
#
# AS builder names this stage "builder"
# We'll reference it later to copy the built JAR
FROM maven:3.9.5-amazoncorretto-17 AS builder

# WORKDIR sets the working directory inside the container
# All subsequent commands run from this directory
# Like running "cd /app" in the terminal
WORKDIR /app

# COPY copies files from host to container
# Copy Maven configuration files first
# Why copy pom.xml separately?
# - Docker caches each layer
# - pom.xml rarely changes
# - If cached, Maven won't re-download dependencies
# - Speeds up subsequent builds significantly
COPY pom.xml .

# Download dependencies
# mvn dependency:go-offline downloads all dependencies
# This creates a cached layer with dependencies
# If pom.xml doesn't change, this layer is reused
# Saves minutes on each build!
RUN mvn dependency:go-offline -B

# Copy source code
# .dockerignore file should exclude:
# - target/ (build output)
# - .git/ (version control)
# - *.log (log files)
COPY src ./src

# Build the application
# mvn clean package:
# - clean: Remove old build artifacts
# - package: Compile code and create JAR file
# -DskipTests: Skip running tests (faster builds)
#
# Note: In production, run tests in CI/CD pipeline before building image
# Don't skip tests there!
#
# The JAR file will be created at:
# target/product-service-1.0.0.jar
RUN mvn clean package -DskipTests

# ============================================================================
# STAGE 2: Runtime Stage
# ============================================================================
# This stage creates the final image with only what's needed to run the app

# FROM specifies base image for final stage
# amazoncorretto:17-alpine provides:
# - Amazon Corretto 17 (Java 17 runtime)
# - Alpine Linux (very lightweight, ~5MB)
#
# Why Alpine?
# - Minimal size (smaller images)
# - Faster downloads
# - Less attack surface
# - Still fully functional
FROM amazoncorretto:17-alpine

# LABEL adds metadata to the image
# Useful for:
# - Documentation
# - Image search
# - Automated processes
LABEL maintainer="your-email@example.com"
LABEL application="product-service"
LABEL version="1.0.0"
LABEL description="Product Service for E-commerce Platform"

# Set working directory in runtime container
WORKDIR /app

# Copy JAR file from builder stage to runtime stage
# --from=builder refers to the "builder" stage
# We copy only the JAR file, not the entire build environment
# This keeps the final image small
#
# Source: /app/target/product-service-1.0.0.jar (from builder)
# Destination: /app/product-service.jar (in final image)
COPY --from=builder /app/target/product-service-1.0.0.jar product-service.jar

# EXPOSE documents which port the container listens on
# This is purely documentation - doesn't actually open the port
# You still need to map ports when running container:
# docker run -p 8081:8081 product-service
#
# Port 8081 matches server.port in application.yml
EXPOSE 8081

# Create a non-root user for security
# Why?
# - Running as root is a security risk
# - If container is compromised, attacker has root access
# - Best practice: always run as non-root user
#
# addgroup creates a group named "spring"
# adduser creates a user named "spring" in the "spring" group
RUN addgroup -S spring && adduser -S spring -G spring

# Change ownership of application files to spring user
# chown: Change owner
# -R: Recursive (all files in /app)
# spring:spring: user:group
RUN chown -R spring:spring /app

# Switch to non-root user
# All subsequent commands and the application run as this user
USER spring

# HEALTHCHECK tells Docker how to check if container is healthy
# Docker will periodically run this command
# If it fails, Docker marks container as unhealthy
#
# Benefits:
# - Automatic restart of unhealthy containers
# - Load balancers can stop sending traffic to unhealthy containers
# - Monitoring systems can alert on unhealthy containers
#
# Options:
# --interval=30s: Check every 30 seconds
# --timeout=3s: Wait max 3 seconds for check to complete
# --start-period=60s: Wait 60s before first check (app startup time)
# --retries=3: Mark unhealthy after 3 consecutive failures
#
# Command: wget --quiet --tries=1 --spider http://localhost:8081/product-service/actuator/health
# - wget: HTTP client (like curl)
# - --quiet: Don't output anything
# - --tries=1: Only try once
# - --spider: Don't download, just check if URL exists
# - URL: Spring Actuator health endpoint
# - Exit code 0: Healthy, Non-zero: Unhealthy
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8081/product-service/actuator/health || exit 1

# ENTRYPOINT defines the command to run when container starts
# exec form (JSON array) is preferred over shell form
# Ensures proper signal handling (SIGTERM for graceful shutdown)
#
# java: Java runtime
# -jar: Run JAR file
# -Xms512m: Initial heap size (512 MB)
# -Xmx1024m: Maximum heap size (1 GB)
# -XX:+UseG1GC: Use G1 garbage collector (good for server applications)
# -Djava.security.egd=file:/dev/./urandom: Faster startup (better randomness)
# -Dspring.profiles.active=prod: Activate production profile (optional)
#
# Note: Adjust memory settings based on your needs
# For development: -Xms256m -Xmx512m
# For production: Depends on load, start with -Xms1g -Xmx2g
ENTRYPOINT ["java", \
    "-Xms512m", \
    "-Xmx1024m", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "product-service.jar"]

# ============================================================================
# How to Build and Run
# ============================================================================
#
# 1. BUILD IMAGE
#    -----------
#    cd product-service
#    docker build -t product-service:1.0.0 .
#
#    Options:
#    -t: Tag the image with name and version
#    .: Build context (current directory)
#
#    This will:
#    - Execute all instructions in this Dockerfile
#    - Create layers
#    - Tag final image as "product-service:1.0.0"
#
# 2. RUN CONTAINER
#    -------------
#    docker run -d \
#      --name product-service \
#      -p 8081:8081 \
#      -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/productdb \
#      -e SPRING_DATASOURCE_USERNAME=root \
#      -e SPRING_DATASOURCE_PASSWORD=root \
#      -e EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka:8761/eureka/ \
#      --network product-service-network \
#      product-service:1.0.0
#
#    Options:
#    -d: Run in detached mode (background)
#    --name: Container name
#    -p: Port mapping (host:container)
#    -e: Environment variables (override application.yml)
#    --network: Connect to Docker network
#
# 3. VIEW LOGS
#    ----------
#    docker logs product-service
#    docker logs -f product-service  (follow logs)
#
# 4. STOP CONTAINER
#    --------------
#    docker stop product-service
#
# 5. REMOVE CONTAINER
#    ----------------
#    docker rm product-service
#
# 6. REMOVE IMAGE
#    ------------
#    docker rmi product-service:1.0.0
#
# ============================================================================
# Docker Compose Integration
# ============================================================================
#
# Add this to docker-compose.yml to run both MySQL and Product Service:
#
# services:
#   mysql-product-service:
#     # ... MySQL configuration ...
#
#   product-service:
#     build:
#       context: .
#       dockerfile: Dockerfile
#     container_name: product-service
#     ports:
#       - "8081:8081"
#     depends_on:
#       mysql-product-service:
#         condition: service_healthy
#     environment:
#       SPRING_DATASOURCE_URL: jdbc:mysql://mysql-product-service:3306/productdb
#       SPRING_DATASOURCE_USERNAME: root
#       SPRING_DATASOURCE_PASSWORD: root
#       EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka:8761/eureka/
#     networks:
#       - product-service-network
#
# Then run:
# docker-compose up --build
#
# ============================================================================
# Best Practices Used in This Dockerfile
# ============================================================================
#
# 1. Multi-stage build (smaller images)
# 2. Layer caching optimization (pom.xml copied separately)
# 3. Non-root user (security)
# 4. Health check (monitoring)
# 5. Proper labels (documentation)
# 6. Alpine base image (minimal size)
# 7. Explicit memory settings (predictable performance)
# 8. ENTRYPOINT in exec form (proper signal handling)
# 9. .dockerignore file (exclude unnecessary files)
# 10. Documented comments (maintainability)
