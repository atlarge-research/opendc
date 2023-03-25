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

LABEL org.opencontainers.image.authors="OpenDC Maintainers <opendc@atlarge-research.com>"
LABEL org.opencontainers.image.url="https://opendc.org"
LABEL org.opencontainers.image.documentation="https://opendc.org"
LABEL org.opencontainers.image.source="https://github.com/atlarge-research/opendc"
LABEL org.opencontainers.image.title="OpenDC Web Runner UI"
LABEL org.opencontainers.image.description="OpenDC Web Runner Docker Image"
LABEL org.opencontainers.image.vendor="AtLarge Research"
