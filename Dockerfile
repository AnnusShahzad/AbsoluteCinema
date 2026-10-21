# Build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon -q > /dev/null
COPY src src
ARG VERSION=1.0.0-SNAPSHOT
RUN ./gradlew bootJar --no-daemon -q -Pversion=${VERSION}

# Runtime stage
FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.title="Absolute Cinema" \
      org.opencontainers.image.description="Full-stack cinema management platform built with Spring Boot" \
      org.opencontainers.image.source="https://github.com/AnnusShahzad/AbsoluteCinema" \
      org.opencontainers.image.licenses="MIT"
RUN useradd --system --uid 10001 cinema
WORKDIR /app
COPY --from=build /app/build/libs/absolute-cinema.jar app.jar
USER cinema
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
