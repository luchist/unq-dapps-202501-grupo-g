# Etapa de construcción
FROM gradle:8.3-jdk17 AS build

# Configuración del entorno
WORKDIR /app
ENV GRADLE_USER_HOME=/app/.gradle
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.parallel=true"

# Copiamos solo los archivos de configuración primero
COPY settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY gradlew .

# Descargamos dependencias
RUN chmod +x gradlew && \
    ./gradlew dependencies --no-daemon || return 0

# Copiamos el resto del código
COPY . .

# Construimos la aplicación
RUN chmod +x gradlew && \
    ./gradlew clean build --no-daemon

# Etapa de ejecución
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]