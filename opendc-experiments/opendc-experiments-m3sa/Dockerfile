FROM eclipse-temurin:21-jdk-jammy AS build

# Install Python 3 + pip for the M3SA analysis
RUN apt-get update && \
    apt-get install -y --no-install-recommends python3 python3-pip python3-venv && \
    rm -rf /var/lib/apt/lists/*

# Copy Gradle wrapper first (for caching)
COPY gradlew /app/
COPY gradle /app/gradle
WORKDIR /app
RUN ./gradlew --version

# Copy the full project and build
COPY ./ /app/
RUN ./gradlew --no-daemon :opendc-experiments:opendc-experiments-m3sa:installDist

# Install Python dependencies
RUN pip3 install --no-cache-dir -r \
    opendc-experiments/opendc-experiments-m3sa/src/main/python/requirements.txt

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-jammy

# Install Python 3 runtime
RUN apt-get update && \
    apt-get install -y --no-install-recommends python3 python3-pip && \
    rm -rf /var/lib/apt/lists/*

# Copy Python packages from the build stage
COPY --from=build /usr/lib/python3 /usr/lib/python3
COPY --from=build /usr/local/lib/python3.10 /usr/local/lib/python3.10

# Copy M3SA JVM distribution
COPY --from=build /app/opendc-experiments/opendc-experiments-m3sa/build/install/opendc-experiments-m3sa \
    /opt/opendc-m3sa

# Copy Python source code
COPY --from=build /app/opendc-experiments/opendc-experiments-m3sa/src/main/python \
    /opt/opendc-m3sa/python

WORKDIR /opt/opendc

# Default entrypoint runs the M3SA CLI
ENTRYPOINT ["/opt/opendc-m3sa/bin/opendc-experiments-m3sa"]

LABEL org.opencontainers.image.title="OpenDC M3SA Runner"
LABEL org.opencontainers.image.description="Multi-Meta-Model Simulation Analysis for OpenDC"
LABEL org.opencontainers.image.source="https://github.com/atlarge-research/opendc"
LABEL org.opencontainers.image.vendor="AtLarge Research"
