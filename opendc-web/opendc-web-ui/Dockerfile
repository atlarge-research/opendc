FROM node:18 AS staging
MAINTAINER OpenDC Maintainers <opendc@atlarge-research.com>

# Copy package details
COPY ./package.json ./package-lock.json /opendc/
RUN cd /opendc && npm ci

# Build frontend
FROM node:18 AS build

COPY ./ /opendc
COPY --from=staging /opendc/node_modules /opendc/node_modules
RUN cd /opendc/ \
    # Environmental variables that will be substituted during image runtime
    && export NEXT_PUBLIC_API_BASE_URL="%%NEXT_PUBLIC_API_BASE_URL%%" \
              NEXT_PUBLIC_SENTRY_DSN="%%NEXT_PUBLIC_SENTRY_DSN%%" \
              NEXT_PUBLIC_AUTH0_DOMAIN="%%NEXT_PUBLIC_AUTH0_DOMAIN%%" \
              NEXT_PUBLIC_AUTH0_CLIENT_ID="%%NEXT_PUBLIC_AUTH0_CLIENT_ID%%" \
              NEXT_PUBLIC_AUTH0_AUDIENCE="%%NEXT_PUBLIC_AUTH0_AUDIENCE%%" \
    && npm run build \
    && npm cache clean --force \
    && mv build/next build/next.template


FROM node:18-slim
COPY --from=build /opendc /opendc
WORKDIR /opendc
CMD ./scripts/envsubst.sh; npm run start
