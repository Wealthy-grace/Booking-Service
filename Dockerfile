## =========================================================
## Multi-stage Dockerfile for Booking-Service
## =========================================================
#
## Stage 1: Build stage
#FROM gradle:8.5-jdk17 AS builder
#
#WORKDIR /app
#
## Copy gradle files first for better caching
#COPY build.gradle settings.gradle ./
#COPY gradle ./gradle
#
## Download dependencies (this layer will be cached if dependencies don't change)
#RUN gradle dependencies --no-daemon || true
#
## Copy source code
#COPY src ./src
#
## Build the application
#RUN gradle clean build -x test --no-daemon
#
## Stage 2: Runtime stage
#FROM eclipse-temurin:17-jre-alpine
#
#WORKDIR /app
#
## Create a non-root user
#RUN addgroup -S spring && adduser -S spring -G spring
#
## Copy the built jar from builder stage
#COPY --from=builder /app/build/libs/*.jar app.jar
#
## Change ownership to non-root user
#RUN chown -R spring:spring /app
#
#USER spring
#
## Expose the application port
#EXPOSE 8084
#
## Health check
#HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
#  CMD wget --quiet --tries=1 --spider http://localhost:8084/actuator/health || exit 1
#
## Run the application
#ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]


# test code
# Multi-stage build for Booking Service
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy gradle files first for better caching
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon --refresh-dependencies || true

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
RUN gradle clean build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose port
EXPOSE 8084

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]