FROM openjdk:14-slim AS staging
MAINTAINER OpenDC Maintainers <opendc@atlarge-research.com>

# Build staging artifacts for dependency caching
COPY ./ /app
WORKDIR /app
RUN mkdir /staging \
    && cp -r buildSrc/ /staging \
    && cp gradle.properties /staging 2>/dev/null | true \
    && find -name "*.gradle.kts" | xargs cp --parents -t /staging

FROM openjdk:14-slim AS builder

# Obtain (cache) Gradle wrapper
COPY gradlew /app/
COPY gradle /app/gradle
WORKDIR /app
RUN ./gradlew --version

# Install (cache) project dependencies only
COPY --from=staging /staging/ /app/
RUN ./gradlew clean build --no-daemon > /dev/null 2>&1 || true

# Build project
COPY ./ /app/
RUN ./gradlew --no-daemon :opendc-runner-web:installDist

FROM openjdk:14-slim
COPY --from=builder /app/opendc-runner-web/build/install /app
WORKDIR /app
CMD opendc-runner-web/bin/opendc-runner-web
