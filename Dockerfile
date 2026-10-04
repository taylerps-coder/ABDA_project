# ==============================================================================
# Multi-stage Dockerfile for Restaurant Discovery & Analytics System (Scala 3 / Cask)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build stage with sbt and Temurin JDK 17
# ------------------------------------------------------------------------------
FROM sbtscala/scala-sbt:eclipse-temurin-17.0.13_11_1.10.7_3.3.4 AS builder

WORKDIR /app

# Cache dependencies by copying build definitions first
COPY project/build.properties project/plugins.sbt ./project/
COPY build.sbt ./
RUN sbt update

# Copy source code and build standalone executable fat JAR
COPY src ./src
RUN sbt assembly

# ------------------------------------------------------------------------------
# Stage 2: Lightweight runtime stage with Temurin JRE 17
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy compiled standalone JAR from builder stage
COPY --from=builder /app/target/scala-3.3.3/Restaurant-Discovery-assembly-1.0.0.jar app.jar

# Copy frontend static assets (served by Cask backend)
COPY frontend ./frontend

# Default port
ENV PORT=8080
EXPOSE 8080

# Run application using Java directly (fast startup, low memory footprint)
CMD ["java", "-jar", "app.jar"]
