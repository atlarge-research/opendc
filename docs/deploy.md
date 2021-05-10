# Deploying OpenDC
This document explains how you can deploy OpenDC in your local environment.
The official way to run OpenDC is using Docker. Other options include building and running locally, and building and
running to deploy on a server.

## Contents

1. [Preamble](#preamble)
1. [Installing Docker](#installing-docker)
1. [Running OpenDC from source](#running-opendc-from-source)

## Preamble

To run OpenDC, you have to create a Google API Console project and client ID, which the OpenDC frontend and
web server will use to authenticate users and requests.
Follow [these steps](https://developers.google.com/identity/sign-in/web/sign-in) to make such a project. In the '
Authorized JavaScript origins' and 'Authorized redirect URI' fields, be sure to add `http://localhost:8080` (frontend)
, `http://localhost:8081` (api) and `https://localhost:3000` (frontend dev). Download the JSON of the OAuth 2.0 client
ID you created from the Credentials tab, and specifically note the `client_id`, which you'll need to build OpenDC.

## Installing Docker

OpenDC uses [Docker](https://www.docker.com/) and [Docker Compose](https://docs.docker.com/compose/) to orchestrate the
deployment of the software stack. Please refer to [Docker Desktop](https://www.docker.com/products/docker-desktop) for
instructions on how install Docker on your machine.

## Running OpenDC from source

To build and run the full OpenDC stack locally on Linux or Mac, you first need to clone the project:

```bash
git clone https://github.com/atlarge-research/opendc.git

# Enter the directory
cd opendc/
```

In the directory you just entered, you need to set up a set of environment variables. To do this, create a file
called `.env` in the `opendc` folder. In this file, replace `your-google-oauth-client-id` with your `client_id` from the
OAuth client ID you created. For a standard setup, you can leave the other settings as-is.

```.env
MONGO_INITDB_ROOT_USERNAME=root
MONGO_INITDB_ROOT_PASSWORD=rootpassword
MONGO_INITDB_DATABASE=admin
OPENDC_DB=opendc
OPENDC_DB_USERNAME=opendc
OPENDC_DB_PASSWORD=opendcpassword
OPENDC_FLASK_SECRET="This is a secret flask key, please change"
OPENDC_OAUTH_CLIENT_ID=your-google-oauth-client-id
OPENDC_API_BASE_URL=http://localhost:8081
```

We provide a set of default traces for you to experiment with. If you want to add others, place them in the `traces`
directory and add entries to the database (see also [the database folder](../database/mongo-init-opendc-db.sh))

If you plan to deploy publicly, please also tweak the other settings. In that case, also check the `docker-compose.yml`
and `docker-compose.prod.yml` for further instructions.

Now, start the server:

```bash
# Build the Docker image
docker-compose build

# Start the containers
docker-compose up
```

Wait a few seconds and open `http://localhost:8080` in your browser to use OpenDC. We recommend Google Chrome for the
best development experience.
