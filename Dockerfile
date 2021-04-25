FROM openjdk:15-slim
MAINTAINER OpenDC Maintainers <opendc@atlarge-research.com>

# Obtain (cache) Gradle wrapper
COPY gradlew /app/
COPY gradle /app/gradle
WORKDIR /app
RUN ./gradlew --version

# Build project
COPY ./ /app/
RUN ./gradlew --no-daemon :opendc-web:opendc-web-runner:installDist

FROM openjdk:15-slim
COPY --from=0 /app/opendc-web/opendc-web-runner/build/install /app
WORKDIR /app
CMD opendc-web-runner/bin/opendc-web-runner
