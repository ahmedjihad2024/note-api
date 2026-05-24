# syntax=docker/dockerfile:1

# ----- Stage 1: build the executable Spring Boot jar -----
# Use a JDK image and the project's own Gradle wrapper for a reproducible build.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy only the build scripts + wrapper first so Docker can cache the dependency
# download layer; it's reused as long as these files don't change.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x ./gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Now copy the source and build. Tests are skipped here on purpose: the embedded
# MongoDB test downloads a large binary and needs network — run tests in CI/locally,
# not inside the image build.
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test \
    && cp "$(ls build/libs/*.jar | grep -v -- '-plain.jar')" /app/app.jar

# ----- Stage 2: slim runtime image -----
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# Run as a non-root user (security best practice).
RUN useradd --system --uid 10001 appuser

# Persisted avatar uploads live here; mount a volume at this path so they survive
# container restarts/redeploys (the app writes to $AVATAR_DIR).
ENV AVATAR_DIR=/data/avatars
RUN mkdir -p /data/avatars && chown -R appuser:appuser /data
VOLUME ["/data/avatars"]

COPY --from=build /app/app.jar app.jar

# Default to the production profile; override any env var at `docker run` time.
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

USER appuser
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
