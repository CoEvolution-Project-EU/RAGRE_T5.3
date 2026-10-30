FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .
COPY src ./src

RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine

WORKDIR /adapter

COPY --from=builder /app/build/libs/co-evolution-adapter-0.0.1-SNAPSHOT.jar ./co-evolution-adapter.jar

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "co-evolution-adapter.jar"]
