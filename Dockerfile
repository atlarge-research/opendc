FROM nikolaik/python-nodejs:python3.8-nodejs14
MAINTAINER OpenDC Maintainers <opendc@atlarge-research.com>

## Dockerfile for the frontend/server part of the deployment

# Installing packages
RUN apt-get update \
	&& apt-get install -y yarn git sed

# Copy OpenDC directory
COPY ./ /opendc

# Fetch web server dependencies
RUN pip install -r /opendc/web-server/requirements.txt

# Build frontend
RUN cd /opendc/frontend \
	&& rm -rf ./build \
	&& yarn \
	&& yarn build

# Set working directory
WORKDIR /opendc

CMD ["sh", "-c", "python web-server/main.py"]
