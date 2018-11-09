FROM node:7.4
MAINTAINER Sacheendra Talluri <sacheendra.t@gmail.com>

# Installing python and web-server dependencies
RUN echo "deb http://ftp.debian.org/debian stretch main" >> /etc/apt/sources.list \
	&& apt-get update \
	&& apt-get install -y python python-pip yarn git sed mysql-client \
	&& rm -rf /var/lib/apt/lists/*

# Copy OpenDC directory
COPY ./ /opendc

# Setting up simulator
RUN python /opendc/opendc-web-server/setup.py install \
	&& chmod 555 /opendc/build/configure.sh \
	&& cd /opendc/opendc-frontend \
	&& rm -rf ./build \
	&& rm -rf ./node_modules \
	&& npm install \
	&& export REACT_APP_OAUTH_CLIENT_ID=$(cat ../keys.json | python -c "import sys, json; print json.load(sys.stdin)['OAUTH_CLIENT_ID']") \
	&& npm run build

# Set working directory
WORKDIR /opendc

CMD ["sh", "-c", "./build/configure.sh && python2.7 opendc-web-server/main.py keys.json"]
