FROM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /app

COPY gradle/ gradle/
COPY gradlew gradlew.bat settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q

COPY src/ src/
RUN ./gradlew installDist --no-daemon -x spotlessCheck

FROM eclipse-temurin:25-jre-noble
WORKDIR /app

COPY --from=builder /app/build/install/voice-chat-transcription/ .

VOLUME /app/model

ENTRYPOINT ["bin/voice-chat-transcription"]
