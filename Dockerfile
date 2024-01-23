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

ENTRYPOINT ["java", "-jar", "EzriqueVoice.jar"]
