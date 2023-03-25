FROM openjdk:17-slim
MAINTAINER OpenDC Maintainers <opendc@atlarge-research.com>

# Obtain (cache) Gradle wrapper
COPY gradlew /app/
COPY gradle /app/gradle
WORKDIR /app
RUN ./gradlew --version

# Build project
ARG OPENDC_AUTH0_DOMAIN
ARG OPENDC_AUTH0_AUDIENCE
ARG OPENDC_AUTH0_DOCS_CLIENT_ID

ENV OPENDC_AUTH0_DOMAIN=$OPENDC_AUTH0_DOMAIN
ENV OPENDC_AUTH0_AUDIENCE=$OPENDC_AUTH0_AUDIENCE
ENV OPENDC_AUTH0_DOCS_CLIENT_ID=$OPENDC_AUTH0_DOCS_CLIENT_ID

COPY ./ /app/
RUN ./gradlew --no-daemon :opendc-web:opendc-web-server:quarkusBuild -Dquarkus.profile=docker

FROM openjdk:17-slim
COPY --from=0 /app/opendc-web/opendc-web-server/build/quarkus-app /opt/opendc
WORKDIR /opt/opendc
CMD java -jar quarkus-run.jar

LABEL org.opencontainers.image.authors="OpenDC Maintainers <opendc@atlarge-research.com>"
LABEL org.opencontainers.image.url="https://opendc.org"
LABEL org.opencontainers.image.documentation="https://opendc.org"
LABEL org.opencontainers.image.source="https://github.com/atlarge-research/opendc"
LABEL org.opencontainers.image.title="OpenDC"
LABEL org.opencontainers.image.description="OpenDC Docker Image"
LABEL org.opencontainers.image.vendor="AtLarge Research"
