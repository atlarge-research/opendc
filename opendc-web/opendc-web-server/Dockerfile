FROM openjdk:17-slim
MAINTAINER OpenDC Maintainers <opendc@atlarge-research.com>

# Obtain (cache) Gradle wrapper
COPY gradlew /app/
COPY gradle /app/gradle
WORKDIR /app
RUN ./gradlew --version

# Build project
COPY ./ /app/
RUN ./gradlew --no-daemon :opendc-web:opendc-web-server:quarkusBuild -Dquarkus.profile=docker

FROM openjdk:17-slim
COPY --from=0 /app/opendc-web/opendc-web-server/build/quarkus-app /opt/opendc
WORKDIR /opt/opendc
CMD java -jar quarkus-run.jar
