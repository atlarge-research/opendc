FROM node:18-slim AS staging
MAINTAINER OpenDC Maintainers <opendc@atlarge-research.com>

# Copy package details
COPY ./package.json ./package-lock.json /opendc/
RUN cd /opendc && npm ci

# Build frontend
FROM node:18-slim AS build

COPY ./ /opendc
COPY --from=staging /opendc/node_modules /opendc/node_modules
RUN cd /opendc/ \
    && npm run build \
    && npm cache clean --force

FROM node:18-slim
COPY --from=build /opendc /opendc
WORKDIR /opendc
CMD npm run start

LABEL org.opencontainers.image.authors="OpenDC Maintainers <opendc@atlarge-research.com>"
LABEL org.opencontainers.image.url="https://opendc.org"
LABEL org.opencontainers.image.documentation="https://opendc.org"
LABEL org.opencontainers.image.source="https://github.com/atlarge-research/opendc"
LABEL org.opencontainers.image.title="OpenDC Web UI"
LABEL org.opencontainers.image.description="OpenDC Web UI Docker Image"
LABEL org.opencontainers.image.vendor="AtLarge Research"
