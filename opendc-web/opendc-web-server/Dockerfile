# syntax=docker/dockerfile:1
#
# The self-hosted server: the distribution `./gradlew :opendc-web:opendc-web-server:installDist`
# builds, which carries the frontend export it serves and the launcher its local dispatcher starts.
# Build that first, then run `docker build -f opendc-web/opendc-web-server/Dockerfile .` from the
# repository root, so the image build needs neither Gradle nor Node.
FROM eclipse-temurin:21-jre

RUN useradd --system --uid 10001 --home-dir /var/lib/opendc --create-home opendc \
    && mkdir -p /var/lib/opendc/data /var/lib/opendc/executions \
    && chown -R 10001:10001 /var/lib/opendc

COPY opendc-web/opendc-web-server/build/install/opendc-server /opt/opendc

WORKDIR /opt/opendc
USER 10001

# The server keeps a quarter of the container's memory, which leaves the local dispatcher's default
# half of the machine to the launchers it starts beside it.
ENV OPENDC_SERVER_OPTS="-XX:MaxRAMPercentage=25" \
    OPENDC_STORAGE_DIRECTORY=/var/lib/opendc/data \
    OPENDC_DISPATCHER_LOCAL_WORK_DIR=/var/lib/opendc/executions

# 8080 serves the app; 9000 serves /q/health and /q/metrics, for probes and Prometheus only.
EXPOSE 8080 9000
ENTRYPOINT ["/opt/opendc/bin/opendc-server"]

LABEL org.opencontainers.image.authors="OpenDC Maintainers <opendc@atlarge-research.com>" \
      org.opencontainers.image.url="https://opendc.org" \
      org.opencontainers.image.documentation="https://opendc.org" \
      org.opencontainers.image.source="https://github.com/atlarge-research/opendc" \
      org.opencontainers.image.title="OpenDC Server" \
      org.opencontainers.image.description="The OpenDC web server, frontend and local launcher" \
      org.opencontainers.image.vendor="AtLarge Research"
