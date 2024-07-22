# Build
FROM openjdk:17-alpine as builder

ENV APP_HOME=/app
WORKDIR $APP_HOME

COPY gradlew $APP_HOME/
COPY gradle $APP_HOME/gradle
COPY gradle.properties $APP_HOME/
COPY build.gradle.kts $APP_HOME/
COPY settings.gradle.kts $APP_HOME/
COPY src $APP_HOME/src

RUN ./gradlew build

# Run
FROM gcr.io/distroless/java17-debian11

ENV APP_HOME=/app
WORKDIR $APP_HOME

COPY --from=builder $APP_HOME/build/libs/*.jar $APP_HOME/
HEALTHCHECK --interval=30s --timeout=30s --start-period=30s \
    --retries=3 CMD curl -f http://localhost:6139/health || exit 1
ENTRYPOINT ["java", "-jar", "EzriqueVoice.jar"]
