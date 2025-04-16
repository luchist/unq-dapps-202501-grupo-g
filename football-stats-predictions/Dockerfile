# Construimos con Gradle JDK17
FROM gradle:8.3-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

# Ejecutamos con JDK17 exponiendo el puerto 8080
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]