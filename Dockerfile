# Build
FROM openjdk:17-alpine as builder

## Set up our working directory
ENV APP_HOME=/app
WORKDIR $APP_HOME

## Copy relevant build files
COPY gradlew $APP_HOME/
COPY gradle $APP_HOME/gradle
COPY gradle.properties $APP_HOME/
COPY build.gradle.kts $APP_HOME/
COPY settings.gradle.kts $APP_HOME/
COPY src $APP_HOME/src

## Build the application
RUN ./gradlew build

# Run
FROM gcr.io/distroless/java17-debian11

## Set up our working directory
ENV APP_HOME=/app
WORKDIR $APP_HOME

## Copy the built JAR file
COPY --from=builder $APP_HOME/build/libs/EzriqueVoice.jar $APP_HOME/EzriqueVoice.jar

## Set up healthchecks
HEALTHCHECK --interval=30s --timeout=30s --start-period=30s \
    --retries=3 CMD curl -f http://localhost:6139/health || exit 1

## Run the application
ENTRYPOINT ["java", "-jar", "EzriqueVoice.jar"]
