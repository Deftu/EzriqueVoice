# Build
FROM openjdk:17-alpine

#ENV APP_HOME=/app
#WORKDIR $APP_HOME

#COPY gradlew $APP_HOME/
#COPY gradle $APP_HOME/gradle
#COPY gradle.properties $APP_HOME/
#COPY build.gradle.kts $APP_HOME/
#COPY settings.gradle.kts $APP_HOME/
#COPY src $APP_HOME/src

#RUN ./gradlew build

# Run
ENV APP_HOME=/app
WORKDIR $APP_HOME

COPY ./build/libs/* $APP_HOME/
COPY ./config.json $APP_HOME/

ENTRYPOINT ["java", "-jar", "EzriqueVoice-0.1.0-all.jar"]
