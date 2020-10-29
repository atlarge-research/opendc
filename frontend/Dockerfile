FROM node:14 AS staging
MAINTAINER OpenDC Maintainers <opendc@atlarge-research.com>

ARG OPENDC_OAUTH_CLIENT_ID
ARG OPENDC_API_BASE_URL

# Copy package details
COPY ./package.json ./yarn.lock /opendc/
RUN cd /opendc && yarn

# Build frontend
FROM node:14 AS build
COPY ./ /opendc
COPY --from=staging /opendc/node_modules /opendc/node_modules
RUN cd /opendc/ \
    && export REACT_APP_OAUTH_CLIENT_ID=$OPENDC_OAUTH_CLIENT_ID \
    && export REACT_APP_API_BASE_URL=$OPENDC_API_BASE_URL \
    && yarn build

# Setup nginx to serve the frontend
FROM nginx:1.19
COPY --from=build /opendc/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
