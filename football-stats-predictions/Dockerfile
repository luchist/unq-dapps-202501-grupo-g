# Building the application
FROM gradle:8.3-jdk17 AS build

# Environment variables
ARG FOOTBALL_API_KEY
ENV FOOTBALL_API_KEY=${FOOTBALL_API_KEY}

WORKDIR /app
ENV GRADLE_USER_HOME=/app/.gradle
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.parallel=true"

# Copy config files
COPY settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY gradlew .

# Dependencies
RUN chmod +x gradlew && \
    ./gradlew dependencies --no-daemon

# Copy source code
COPY . .

# Build the application
RUN chmod +x gradlew && \
    ./gradlew clean build --no-daemon --scan

# Execute
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]