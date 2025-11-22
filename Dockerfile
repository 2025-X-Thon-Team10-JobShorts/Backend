# Multi-stage build for Spring Boot application
FROM gradle:8-jdk17 AS build

WORKDIR /app

# Copy Gradle files
COPY server/gradle ./gradle
COPY server/gradlew ./
COPY server/gradlew.bat ./
COPY server/gradle.properties* ./
COPY server/build.gradle ./
COPY server/settings.gradle ./

# Grant execute permission to gradlew
RUN chmod +x ./gradlew

# Copy source code
COPY server/src ./src

# Build the application (skip tests, no daemon)
RUN ./gradlew clean build -x test --no-daemon

# Find the built jar file
RUN find /app/build/libs -name "*.jar" -not -name "*-plain.jar" -exec cp {} /app/app.jar \;

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/app.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/api/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

