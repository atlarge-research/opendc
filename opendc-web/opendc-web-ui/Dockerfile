FROM node:15 AS staging
MAINTAINER OpenDC Maintainers <opendc@atlarge-research.com>

# Copy package details
COPY ./package.json ./yarn.lock /opendc/
RUN cd /opendc && yarn

# Build frontend
FROM node:15 AS build

COPY ./ /opendc
COPY --from=staging /opendc/node_modules /opendc/node_modules
RUN cd /opendc/ \
    && yarn build \
    && mv build/index.html build/index.html.template

# Setup nginx to serve the frontend
FROM nginx:1.20
COPY --from=build /opendc/scripts/envsubst-html.sh /docker-entrypoint.d/00-envsubst-html.sh
COPY --from=build /opendc/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
