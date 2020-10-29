FROM openjdk:15-slim
MAINTAINER OpenDC Maintainers <opendc@atlarge-research.com>

# Obtain (cache) Gradle wrapper
COPY gradlew /app/
COPY gradle /app/gradle
WORKDIR /app
RUN ./gradlew --version

# Build project
COPY ./ /app/
RUN ./gradlew --no-daemon :opendc-runner-web:installDist

FROM openjdk:15-slim
COPY --from=0 /app/opendc-runner-web/build/install /app
WORKDIR /app
CMD opendc-runner-web/bin/opendc-runner-web
