FROM node:7.4
MAINTAINER Sacheendra Talluri <sacheendra.t@gmail.com>

# Installing python, yarn, and web-server dependencies
RUN curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | apt-key add - \
	&& echo "deb http://dl.yarnpkg.com/debian/ stable main" | tee /etc/apt/sources.list.d/yarn.list \
	&& echo "deb http://ftp.debian.org/debian stretch main" >> /etc/apt/sources.list \
	&& apt-get update \
	&& apt-get -y -t stretch install gcc-6 g++-6 \
	&& apt-get install -y python python-pip yarn git sqlite3 sed supervisor mysql-client \
	&& pip install oauth2client eventlet flask-socketio \
	&& rm -rf /var/lib/apt/lists/*

# Copy OpenDC directory
COPY ./ /opendc

# Setting up simulator
RUN mkdir -p /data/database \
	&& chmod 555 /opendc/build/configure.sh \
	&& cd /opendc/opendc-simulator/Simulator \
	&& rm -f ./simulator ./sqlite3.o \
	&& make \
	&& chmod 555 ./simulator \
	&& git config --global url."https://".insteadOf git:// \
	&& cd /opendc/opendc-frontend \
	&& rm -rf ./build \
	&& rm -rf ./node_modules \
	&& yarn \
	&& export REACT_APP_OAUTH_CLIENT_ID=$(cat ../keys.json | python -c "import sys, json; print json.load(sys.stdin)['OAUTH_CLIENT_ID']") \
	&& yarn build

CMD ["sh", "-c", "cd /opendc && ./build/configure.sh /data/database && /usr/bin/supervisord -c /opendc/build/supervisord.conf"]

VOLUME ["/data/database"]
