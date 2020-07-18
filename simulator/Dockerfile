FROM gradle:jdk14
MAINTAINER OpenDC Maintainers <opendc@atlarge-research.com>

COPY ./ /simulator
RUN cd /simulator/ \
    && gradle --no-daemon :opendc:opendc-runner-web:installDist

FROM openjdk:14
COPY --from=0 /simulator/opendc/opendc-runner-web/build/install /simulator
WORKDIR /simulator
CMD opendc-runner-web/bin/opendc-runner-web
