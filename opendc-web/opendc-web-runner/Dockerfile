FROM openjdk:18-slim
MAINTAINER OpenDC Maintainers <opendc@atlarge-research.com>

# Obtain (cache) Gradle wrapper
COPY gradlew /app/
COPY gradle /app/gradle
WORKDIR /app
RUN ./gradlew --version

# Build project
COPY ./ /app/
RUN ./gradlew --no-daemon :opendc-web:opendc-web-runner:installDist

FROM openjdk:17-slim
COPY --from=0 /app/opendc-web/opendc-web-runner/build/install /opt/
COPY --from=0 /app/traces /opt/opendc/traces
WORKDIR /opt/opendc
CMD bin/opendc-web-runner
