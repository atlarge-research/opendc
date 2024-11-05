---
sidebar_position: 3
---

# Deploying OpenDC
This document explains how you can deploy a multi-tenant instance of OpenDC using Docker.

## Contents

1. [Setting up Auth0](#setting-up-auth0)
1. [Installing Docker](#installing-docker)
1. [Running OpenDC from source](#running-opendc-from-source)

## Setting up Auth0

OpenDC uses [Auth0](https://auth0.com) as Identity Provider so that OpenDC does not have to manage user data itself,
which greatly simplifies our frontend and backend implementation. We have chosen to use Auth0 as it is a well-known
Identity Provider with good software support and a free tier for users to experiment with.

To deploy OpenDC yourself, you need to have an [Auth0 tenant](https://auth0.com/docs/get-started/learn-the-basics) and
create:

1. **An API**  
   You need to define the OpenDC API server in Auth0. Please refer to the [following guide](https://auth0.com/docs/quickstart/backend/python/01-authorization#create-an-api)
   on how to define an API in Auth0.

   Remember the identifier you created the API with, as we need it in the next steps (as `OPENDC_AUTH0_AUDIENCE`).
2. **A Single Page Application (SPA)**  
   You need to define the OpenDC frontend application in Auth0. Please see the [following guide](https://auth0.com/docs/quickstart/spa/react#configure-auth0)
   on how you can define an SPA in Auth0. Make sure you have added the necessary URLs to the _Allowed Callback URLs_:
   for a local deployment, you should add at least `http://localhost:3000, http://localhost:8080`.

   Once your application has been created, you should have a _Domain_ and _Client ID_ which we need to pass to the
   frontend application (as `OPENDC_AUTH0_DOMAIN` and `OPENDC_AUTH0_CLIENT_ID` respectively).


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
called `.env` in the `opendc` folder. In this file, replace `your-auth0-*` with the Auth0 details you got from the first
step. For a standard setup, you can leave the other settings as-is.

```.env
OPENDC_DB_USERNAME=opendc
OPENDC_DB_PASSWORD=opendcpassword
OPENDC_AUTH0_DOMAIN=your-auth0-domain
OPENDC_AUTH0_CLIENT_ID=your-auth0-client-id
OPENDC_AUTH0_AUDIENCE=your-auth0-api-identifier
OPENDC_API_BASE_URL=http://web
```

We provide a set of default traces for you to experiment with. If you want to add others, place them in the `traces`
directory and add entries to the database (see also [the SQL init script](https://github.com/atlarge-research/opendc/tree/master/opendc-web/opendc-web-server/src/main/resources/db/migration/V1.0.0__core.sql))

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
best user experience.
