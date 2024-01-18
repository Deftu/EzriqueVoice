FROM gcr.io/distroless/java17-debian11

WORKDIR /app

COPY gradlew /app/
COPY gradle /app/gradle
COPY gradle.properties /app/
COPY build.gradle.kts /app/
COPY settings.gradle.kts /app/
COPY src /app/src

RUN ./gradlew build

COPY build/libs/*.jar /app/build/

ENTRYPOINT ["java", "-jar", "/app/build/libs/EzriqueVoice-0.1.0-all.jar"]
