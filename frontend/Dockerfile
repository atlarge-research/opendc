FROM node:14
MAINTAINER OpenDC Maintainers <opendc@atlarge-research.com>

ARG REACT_APP_OAUTH_CLIENT_ID

# Copy OpenDC directory
COPY ./ /opendc

# Build frontend
RUN cd /opendc/ \
    && rm -rf ./build \
    && yarn \
    && export REACT_APP_OAUTH_CLIENT_ID=$REACT_APP_OAUTH_CLIENT_ID \
    && yarn build

# Setup nginx to serve the frontend
FROM nginx:1.19
COPY --from=0 /opendc/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
