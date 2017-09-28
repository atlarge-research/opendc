FROM node:7.4
MAINTAINER Sacheendra Talluri <sacheendra.t@gmail.com>

# Installing python, yarn, and web-server dependencies
RUN curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | apt-key add - \
	&& echo "deb http://dl.yarnpkg.com/debian/ stable main" | tee /etc/apt/sources.list.d/yarn.list \
	&& echo "deb http://ftp.debian.org/debian stretch main" >> /etc/apt/sources.list \
	&& apt-get update \
	&& apt-get install -y python python-pip yarn git sed supervisor openjdk-8-jdk mysql-client \
	&& pip install oauth2client eventlet flask-socketio mysql-connector-python-rf \
	&& rm -rf /var/lib/apt/lists/*

# Copy OpenDC directory
COPY ./ /opendc

# Setting up simulator
RUN chmod 555 /opendc/build/configure.sh \
	&& cd /opendc/opendc-simulator \
	&& ./gradlew build \
	&& cd /opendc/opendc-frontend \
	&& rm -rf ./build \
	&& rm -rf ./node_modules \
	&& npm install \
	&& export REACT_APP_OAUTH_CLIENT_ID=$(cat ../keys.json | python -c "import sys, json; print json.load(sys.stdin)['OAUTH_CLIENT_ID']") \
	&& npm run build

CMD ["sh", "-c", "cd /opendc && ./build/configure.sh && /usr/bin/supervisord -c /opendc/build/supervisord.conf"]
