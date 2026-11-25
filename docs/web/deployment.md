---
description: How to deploy the OpenDC server with Docker Compose, without Docker, on Kubernetes and with a SLURM cluster.
---

# Deployment

This page explains how to install a deployment of the OpenDC web module. It starts with a single machine, which is
enough for a person or a small group, and continues with sign-in, Kubernetes and SLURM. The files it refers to are in
the root of the repository and in `deploy/`.

## What a deployment consists of

The server distribution is the whole self-hosted product: the REST API, the web application it serves from the same
origin, and the launcher that simulates. A deployment adds three services around it:

| Service         | Purpose                                   | Options                                              |
|-----------------|-------------------------------------------|------------------------------------------------------|
| Database        | accounts, projects, experiments and runs  | PostgreSQL                                           |
| Object store    | traces and results                        | a directory, or an S3-compatible bucket              |
| Telemetry store | progress of running simulations           | the memory of the server, or Redis                   |

One setting, `OPENDC_EXECUTION_DISPATCHER`, selects where simulations run: `local` runs them beside the server,
`kubernetes` as Jobs, and `slurm` as batch jobs. [How Experiments Execute](execution.md#dispatchers) compares the three.

## On a single machine

Docker Compose starts the server and PostgreSQL from a checkout of the repository:

``` bash
./gradlew :opendc-web:opendc-web-server:installDist
cp .env.example .env                  # set OPENDC_DB_PASSWORD
docker compose up -d --build          # docker-compose.override.yml builds the image
deploy/smoke-test.sh                  # runs an experiment end to end
```

The web application is then at <http://localhost:8080>. This stack signs nobody in and keeps traces and results on a
Docker volume. `docker compose down -v` stops the stack and deletes its data.

Simulations run inside the server container. Three settings in `.env` bound them:

| Variable                      | Default | Meaning                                                         |
|-------------------------------|---------|-----------------------------------------------------------------|
| `OPENDC_MEMORY_LIMIT`         | `4g`    | the memory of the server container, simulations included        |
| `OPENDC_SIMULATION_MEMORY_MB` | `2048`  | the memory that all launchers share, in MiB                     |
| `OPENDC_SIMULATION_CORES`     | `2`     | the cores that all launchers share                              |

### With S3 storage and Redis

A deployment of several servers cannot keep its files on one machine. The overlay `docker-compose.s3.yml` keeps traces
and results in a bucket and progress in Redis instead. It starts [MinIO](https://min.io) and Redis, and needs
`MINIO_ROOT_PASSWORD` in `.env`:

``` bash
docker compose -f docker-compose.yml -f docker-compose.override.yml -f docker-compose.s3.yml up -d
```

With S3 storage, browsers upload traces to the bucket and download results from it directly. The bucket therefore has
to accept `PUT` and `GET` requests from the origin of the deployment, and `OPENDC_PUBLIC_S3_ENDPOINT` has to be the
address at which browsers reach the bucket.

### Without Docker

`installDist` writes the distribution to `opendc-web/opendc-web-server/build/install/opendc-server`. Its `config/`
directory points the server at the web application and the launcher beside it, so the server has to start from that
directory:

``` bash
cd opendc-web/opendc-web-server/build/install/opendc-server
OPENDC_AUTH_MODE=anonymous \
OPENDC_DB_URL=jdbc:postgresql://localhost:5432/opendc \
OPENDC_DB_USERNAME=opendc OPENDC_DB_PASSWORD=... \
bin/opendc-server
```

The server needs Java&nbsp;21 and a PostgreSQL database. It creates and migrates the database schema when it starts.

### Images

The server and the launcher each have an image. Both package a distribution that Gradle has already built, so building
an image needs neither Gradle nor Node.js:

``` bash
./gradlew :opendc-web:opendc-web-server:installDist :opendc-web:opendc-web-launcher:installDist
docker build -f opendc-web/opendc-web-server/Dockerfile -t opendc-server .
docker build -f opendc-web/opendc-web-launcher/Dockerfile -t opendc-launcher .
```

Releases publish both images as `ghcr.io/atlarge-research/opendc-server` and
`ghcr.io/atlarge-research/opendc-launcher`. The launcher has to be the same version as the server, because the manifest
that the server writes for it is their contract.

### Hosting the web application elsewhere

The web application is a set of static files and can be served from another origin than the API. To build it for an
API at `https://api.example.org`:

``` bash
cd opendc-web/opendc-web-frontend
NEXT_PUBLIC_API_BASE_URL=https://api.example.org pnpm build
```

The host of the resulting `out/` directory has to fall back from `/page` to `/page.html`, which in nginx is
`try_files $uri $uri.html $uri/ =404`. The server accepts the other origin with `QUARKUS_HTTP_CORS_ENABLED=true` and
`QUARKUS_HTTP_CORS_ORIGINS=https://app.example.org`.

## Signing people in

`OPENDC_AUTH_MODE` selects how people sign in. In `auth0` mode, the default, they sign in through
[Auth0](https://auth0.com). In `anonymous` mode nobody signs in and every request acts as one administrator. The web
application reads the mode from `GET /api/v1/config`, so one build serves both.

Auth0 needs two objects:

1. An API. Its identifier is the *audience* that the server checks, such as `https://opendc.example.org/api`. *Allow
   Offline Access* has to be on, or sessions end when the first access token expires.
2. A single-page application with access to that API and with refresh-token rotation on. The origin of the deployment,
   such as `https://opendc.example.org` without a trailing slash, has to be an allowed callback URL, logout URL and web
   origin.

The server then needs the following settings:

| Variable                       | Value                                                              |
|--------------------------------|--------------------------------------------------------------------|
| `OPENDC_AUTH_MODE`             | `auth0`                                                            |
| `QUARKUS_OIDC_TENANT_ENABLED`  | `true`                                                             |
| `QUARKUS_OIDC_AUTH_SERVER_URL` | `https://<tenant>.auth0.com/`, with the trailing slash             |
| `QUARKUS_OIDC_TOKEN_AUDIENCE`  | the identifier of the API                                          |
| `OPENDC_AUTH_AUTH0_CLIENT_ID`  | the client ID of the single-page application                       |
| `OPENDC_AUTH_ADMINS`           | the Auth0 subjects (`auth0\|...`) of the first administrators, separated by commas |

The server refuses to start when the mode and `QUARKUS_OIDC_TENANT_ENABLED` disagree. With Docker Compose, the overlay
`docker-compose.prod.yml` sets these variables from `OPENDC_AUTH0_DOMAIN`, `OPENDC_AUTH0_AUDIENCE`,
`OPENDC_AUTH0_CLIENT_ID` and `OPENDC_AUTH_ADMINS` in `.env`.

The server creates an account the first time a person signs in. Personal access tokens work in both modes.

## On Kubernetes

`deploy/kubernetes` is a [kustomization](https://kubectl.docs.kubernetes.io/references/kustomize/). It contains the
server, the permissions its dispatcher needs, the service account that execution pods run as, a network policy that
closes those pods to inbound traffic, a Redis without persistence for progress and an ingress. It expects a PostgreSQL
database and an S3-compatible bucket that already exist.

To deploy it:

1. Copy `secrets.example.env` to `secrets.env`. Fill in the database, the credentials of the bucket and a signing key
   for download links, which `openssl rand -base64 32` generates.
2. In `server.env`, set the endpoint of the bucket as pods and as browsers reach it, the Auth0 settings, and the
   launcher image. Also set the most one execution may use, `SLOT_CORES` and `SLOT_MEMORY_MB`, and the number of
   executions that may run at once.
3. In `ingress.yaml`, set the host, the ingress class and the TLS secret. In `kustomization.yaml`, pin the server image
   to the version of the launcher image.
4. Run `kubectl apply -k deploy/kubernetes`.

`kubectl -n opendc get jobs,pods` shows the executions while an experiment runs.

Execution pods read their inputs and write their outputs through signed URLs, so they need to reach the bucket but hold
no credentials. The server refuses to start on Kubernetes with local storage, or with a telemetry address that pods
cannot reach, because executions would have nowhere to read from or report to.

The optional node reader in `rbac.yaml` lets the dispatcher read the nodes and every pod placed on them. It then shrinks
each execution to what the roomiest node has left beside the pods it already runs. Without the node reader, an
execution asks for `SLOT_CORES` and `SLOT_MEMORY_MB` as they are, so they have to fit what one node has left after its
DaemonSets and system pods. On k3s, which reserves nothing, a slot the size of a whole machine waits for a node that
never comes.

## With a SLURM cluster

The SLURM dispatcher submits each execution as a batch job over SSH. It assumes that compute nodes reach nothing but
their shared file system. The server therefore copies the launcher, the traces and the manifest to the cluster over
SFTP, and copies the results of each run back. A deployment with this dispatcher has to run a single server.

The cluster needs three things before the first run:

- a key without a passphrase for the head node, and for the jump host if there is one, in a file that only the server
  can read;
- the host keys of both machines in a known-hosts file, because the server trusts no other host;
- a Java&nbsp;21 runtime on the shared file system. The server checks its version before use.

The profile `das5` configures the [DAS-5](https://www.cs.vu.nl/das5/) cluster, reached through a jump host:

``` bash
OPENDC_SLURM_USER=<das5 user> OPENDC_VUNET_ID=<vunet id> \
OPENDC_SLURM_IDENTITY_FILE=/run/secrets/das5_key \
OPENDC_SLURM_KNOWN_HOSTS=/run/secrets/known_hosts \
QUARKUS_PROFILE=prod,das5 bin/opendc-server
```

The profile expects the Java runtime at `/var/scratch/<user>/opendc/jdk-21`. It keeps jobs within the 15-minute daytime
limit of DAS-5 on nodes with 16&nbsp;cores. Its node memory of 56,000&nbsp;MiB is an assumption that `sinfo -o "%n %m"`
verifies. Another cluster takes the same settings under
[`opendc.dispatcher.slurm.*`](configuration.md#slurm-dispatcher).

## Upgrading from the previous web application

This version starts the database schema afresh. Nothing migrates the database of the previous `opendc-web`, and the
keys of the Compose files have changed. An upgrade therefore starts from `.env.example` and the Compose files in the
root of the repository, not from an old `.env`.
