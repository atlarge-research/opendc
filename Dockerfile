FROM node:14.2.0
MAINTAINER Sacheendra Talluri <sacheendra.t@gmail.com>

# Adding the mongodb repo and installing the client
RUN wget -qO - https://www.mongodb.org/static/pgp/server-4.2.asc | apt-key add - \
	&& echo "deb http://repo.mongodb.org/apt/debian stretch/mongodb-org/4.2 main" | tee /etc/apt/sources.list.d/mongodb-org-4.2.list \
	&& apt-get update \
	&& apt-get install -y mongodb-org

# Installing python and web-server dependencies
RUN echo "deb http://ftp.debian.org/debian stretch main" >> /etc/apt/sources.list \
	&& apt-get update \
	&& apt-get install -y python3 python3-pip yarn git sed mysql-client pymongo \
	&& pip3 install oauth2client eventlet flask-socketio flask-compress mysql-connector-python-rf \
	&& pip3 install --upgrade pyasn1-modules \
	&& rm -rf /var/lib/apt/lists/*

# Copy OpenDC directory
COPY ./ /opendc

# Setting up simulator
RUN chmod 555 /opendc/build/configure.sh \
	&& cd /opendc/opendc-frontend \
	&& rm -rf ./build \
	&& rm -rf ./node_modules \
	&& npm install \
	&& export REACT_APP_OAUTH_CLIENT_ID=$(cat ../keys.json | python -c "import sys, json; print json.load(sys.stdin)['OAUTH_CLIENT_ID']") \
	&& npm run build

# Set working directory
WORKDIR /opendc

CMD ["sh", "-c", "./build/configure.sh && python3 opendc-web-server/main.py keys.json"]
