# syntax=docker/dockerfile:1
#
# The program one execution runs on Kubernetes. Build the distribution with
# `./gradlew :opendc-web:opendc-web-launcher:installDist`, then run
# `docker build -f opendc-web/opendc-web-launcher/Dockerfile .` from the repository root.
#
# Its contract with the Kubernetes dispatcher: the manifest arrives as MANIFEST_URL, the heap as
# -Xmx in OPENDC_LAUNCHER_OPTS, the working directory is /tmp (where the pod mounts its scratch
# volume over a read-only root), and the user is numeric so runAsNonRoot can verify it.
FROM eclipse-temurin:21-jre

RUN useradd --system --uid 10001 --no-create-home --home-dir /tmp launcher

COPY opendc-web/opendc-web-launcher/build/install/opendc-launcher /opt/opendc-launcher

WORKDIR /tmp
USER 10001

ENTRYPOINT ["/opt/opendc-launcher/bin/opendc-launcher"]

LABEL org.opencontainers.image.authors="OpenDC Maintainers <opendc@atlarge-research.com>" \
      org.opencontainers.image.url="https://opendc.org" \
      org.opencontainers.image.source="https://github.com/atlarge-research/opendc" \
      org.opencontainers.image.title="OpenDC Launcher" \
      org.opencontainers.image.description="Runs one execution of OpenDC simulations from a manifest" \
      org.opencontainers.image.vendor="AtLarge Research"
