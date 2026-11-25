# Deploying OpenDC

The server distribution is the whole self-hosted product: the REST API, the frontend it serves from
the same origin, and the launcher that runs simulations. An *execution* is one launcher process,
which runs one or more simulations. One setting, `OPENDC_EXECUTION_DISPATCHER`, chooses where
executions run: `local` (beside the server), `kubernetes` (one Job each) or `slurm` (one batch job
each, submitted over SSH).

## Run it from a checkout

```sh
./gradlew :opendc-web:opendc-web-server:installDist
cp .env.example .env                  # set OPENDC_DB_PASSWORD
docker compose up -d --build          # docker-compose.override.yml builds the image
deploy/smoke-test.sh                  # runs an experiment end to end
```

Then open <http://localhost:8080>. This stack signs nobody in (`OPENDC_AUTH_MODE=anonymous`) and
keeps traces and results on a volume. Simulations run inside the server container, which is limited
to `OPENDC_MEMORY_LIMIT` (4 GiB); the launchers share `OPENDC_SIMULATION_MEMORY_MB` (2 GiB) of it.

The S3 overlay keeps traces and results in a bucket and live progress in Redis instead. It starts
MinIO and Redis, and needs `MINIO_ROOT_PASSWORD` in `.env`:

```sh
docker compose -f docker-compose.yml -f docker-compose.override.yml -f docker-compose.s3.yml up -d
```

`docker compose down -v` stops everything and deletes its data.

### Without Docker

Start the distribution in `opendc-web/opendc-web-server/build/install/opendc-server` with
`bin/opendc-server` from that directory, whose `config/` points the server at the frontend and the
launcher beside it. The server needs a PostgreSQL database, given by `OPENDC_DB_URL`,
`OPENDC_DB_USERNAME` and `OPENDC_DB_PASSWORD`. CI starts it this way against PostgreSQL on every
build. For development, see [`opendc-web`](../opendc-web/README.md).

### Images

Both images package a distribution that Gradle has already built, so building an image needs
neither Gradle nor Node:

```sh
./gradlew :opendc-web:opendc-web-server:installDist :opendc-web:opendc-web-launcher:installDist
docker build -f opendc-web/opendc-web-server/Dockerfile -t opendc-server .
docker build -f opendc-web/opendc-web-launcher/Dockerfile -t opendc-launcher .
```

The Kubernetes dispatcher runs the launcher image, one Job per execution. The launcher must be the
same version as the server, because the manifest it receives is their contract.

### Hosting the frontend elsewhere

Build the export with `NEXT_PUBLIC_API_BASE_URL=https://api.example.org pnpm build`, and host `out/`
with a fallback from `/page` to `/page.html` (nginx: `try_files $uri $uri.html $uri/ =404`). Then
let the server accept the other origin with `QUARKUS_HTTP_CORS_ENABLED=true` and
`QUARKUS_HTTP_CORS_ORIGINS=https://app.example.org`.

## Sign people in

`OPENDC_AUTH_MODE` selects how people sign in. In `auth0` mode, the default, they sign in through
Auth0. In `anonymous` mode nobody signs in and every request acts as one administrator, which suits
a single person running OpenDC for themselves. The frontend reads the mode from `GET /api/v1/config`,
so one build serves both.

For Auth0, create:

1. an API, whose identifier is the audience the server checks (e.g. `https://opendc.example.org/api`);
2. a single-page application (SPA) with access to that API, refresh-token rotation on, and the
   deployment's origin (`https://opendc.example.org`, no trailing slash) as an allowed callback URL,
   logout URL and web origin.

Turn on "Allow Offline Access" for the API, or sessions end when the first access token expires.

Then give the server:

| Variable | Value |
|---|---|
| `OPENDC_AUTH_MODE` | `auth0` |
| `QUARKUS_OIDC_TENANT_ENABLED` | `true` |
| `QUARKUS_OIDC_AUTH_SERVER_URL` | `https://<tenant>.auth0.com/`, with the trailing slash |
| `QUARKUS_OIDC_TOKEN_AUDIENCE` | the API's identifier |
| `OPENDC_AUTH_AUTH0_CLIENT_ID` | the SPA's client ID, which the frontend signs in with |
| `OPENDC_AUTH_ADMINS` | comma-separated Auth0 subjects (`auth0\|...`) of the first administrators |

The server refuses to start when the mode and `QUARKUS_OIDC_TENANT_ENABLED` disagree. With Compose,
`docker-compose.prod.yml` sets these variables from `OPENDC_AUTH0_DOMAIN` (the tenant's URL),
`OPENDC_AUTH0_AUDIENCE`, `OPENDC_AUTH0_CLIENT_ID` and `OPENDC_AUTH_ADMINS` in `.env`; the keys are
those the old web app used.

The server creates an account the first time someone signs in. Its owner picks a handle once, and
their traces are named after it (`alice/nightly`). From the account menu, people create personal
access tokens (`odc_pat_...`) for scripts and the CLI. Tokens work in both modes:

```sh
OPENDC_TOKEN=odc_pat_... opendc run experiment.json --api-url https://opendc.example.org
```

## Run on Kubernetes

`deploy/kubernetes` is a kustomization. It contains the server, the permissions its dispatcher needs,
the service account that execution pods run as, a NetworkPolicy that closes those pods to inbound
traffic, an ephemeral Redis for live progress, and an ingress. It expects an existing PostgreSQL
database and S3-compatible bucket.

1. Copy `secrets.example.env` to `secrets.env`, and fill in the database, the bucket's credentials
   and a download signing key (`openssl rand -base64 32`).
2. In `server.env`, set the bucket's endpoint as pods and browsers reach it, the Auth0 settings
   above, the launcher image, the most one execution may use (`SLOT_CORES` and `SLOT_MEMORY_MB`)
   and how many executions may run at once.
3. In `ingress.yaml`, set your host, ingress class and TLS secret. In `kustomization.yaml`, pin the
   server image to the launcher image's version.
4. Run `kubectl apply -k deploy/kubernetes`, and watch `kubectl -n opendc get jobs,pods` while an
   experiment runs.

The bucket must accept `PUT` and `GET` from the deployment's origin, because browsers upload traces
and download results directly. Execution pods must reach the bucket too; they read their inputs and
write their outputs through signed URLs, so they need no credentials of their own.

The server refuses to start on Kubernetes with local storage, or with a telemetry URL that pods
cannot reach, since executions would have nowhere to read from or report to.

The optional node reader in `rbac.yaml` lets the dispatcher read the nodes and every pod placed on
them, so it shrinks each execution to what the roomiest node has left beside the pods it already
runs. Without it, `SLOT_CORES` and `SLOT_MEMORY_MB` are asked for as they are, so they must fit what
one node has left after its DaemonSets and system pods: on k3s, which reserves nothing, a slot the
size of a whole machine is never scheduled. The admin panel then shows the dispatcher's own ceiling
instead of the cluster's capacity.

## Run on a SLURM cluster

The SLURM dispatcher submits each execution as a batch job over SSH. It assumes that compute nodes
reach nothing but their shared filesystem, so the server stages the launcher, the traces and a
rewritten manifest there over SFTP, and copies each finished run back itself. For the same reason, a
SLURM run shows no live progress, only its outcome. Run a single server replica.

The `das5` profile configures DAS-5, reached through the VU bastion:

```sh
OPENDC_SLURM_USER=<das5 user> OPENDC_VUNET_ID=<vunet id> \
OPENDC_SLURM_IDENTITY_FILE=/run/secrets/das5_key \
OPENDC_SLURM_KNOWN_HOSTS=/run/secrets/known_hosts \
QUARKUS_PROFILE=prod,das5 bin/opendc-server
```

Before the first run:

- put an unencrypted key for both the bastion and the head node in the identity file, readable by
  the server only;
- put both host keys in the known-hosts file, since the server trusts no other host;
- install a Java 21 runtime at `/var/scratch/<user>/opendc/jdk-21` on the shared filesystem, whose
  version the server checks before use.

The profile holds jobs to DAS-5's 15-minute daytime limit on 16-core nodes. Its node memory
(`slot-memory-mb=56000`) is an assumption; check it with `sinfo -o "%n %m"` before relying on it.
Another cluster takes the same keys under `opendc.dispatcher.slurm.*`: `host`, `user`, `jump-host`,
`remote-root`, `java`, `partition`, `sbatch-options`, `slot-cores`, `slot-memory-mb`, `time-cap` and
`max-jobs`.

## Operate it

### Health and metrics

Health checks and Prometheus metrics live on management port 9000, which the ingress never routes:
`/q/health/live`, `/q/health/ready`, `/q/health/started` and `/q/metrics`. Besides the JVM's metrics
and the API's request timings, the server exports:

| Metric | Labels | Meaning |
|---|---|---|
| `opendc_executions_launched_total` | `outcome`: accepted, rejected, unavailable | executions handed to the platform |
| `opendc_executions_settled_total` | `reason`: ok, simulation_error, oom, timeout, walltime, cancelled, rejected, unknown, invalid_spec | executions that ended, by cause |
| `opendc_executions` | `state`: queued, submitted, running, succeeded, failed, cancelled | executions in each state |
| `opendc_units_queued` | | runs waiting for an execution |
| `opendc_platform_cores_total`, `opendc_platform_cores_allocated` | | the platform's capacity and how much of it is in use |

The server reads the gauges from the database every 15 s, so every replica reports the same numbers:
take the maximum across replicas, not the sum.

`deploy/grafana/opendc.json` is a Grafana dashboard of these metrics, plus API traffic, latency and
heap. Import it, and pick the Prometheus data source that scrapes port 9000. A test in the server's
suite checks that every query in the dashboard names a metric and labels that the server exports.

### Trace imports

People can import a trace from public URLs, one per table, which the server fetches itself. It
refuses URLs that resolve to private, loopback, link-local or other non-public addresses, and checks
every redirect the same way. It gives up on a table larger than `OPENDC_TRACES_IMPORT_MAX_BYTES`
(10 GiB), on a remote server that stays silent for two minutes, and on an import older than
`OPENDC_TRACES_IMPORT_DEADLINE` (6 h).

The server checks a host before each request but resolves it again to connect, so a host whose DNS
answer changes in between gets through. Where the server can reach sensitive internal services,
restrict its egress at the network as well. An intranet deployment that imports from its own
mirrors sets `OPENDC_TRACES_IMPORT_ALLOW_PRIVATE_HOSTS=true`.

### Administrators and plans

Administrators are the accounts in `OPENDC_AUTH_ADMINS`, or everybody in anonymous mode. Their
account menu opens an admin panel that lists the accounts, the platform's capacity, and executions
on and off the platform with their runs and launcher logs. From there an administrator can rerun a
failed execution past the automatic retry limit, for a failure known to be transient.
Administrators cannot see other people's projects.

Every account has a plan, which caps its simulation time per 5-hour session and per week:

| Plan | Per session | Per week |
|---|---|---|
| free | 1 h | 10 h |
| education | 4 h | 40 h |
| enterprise | unlimited | unlimited |

Plans change in the database only. A budget window copies its plan's caps when it opens, so change
both:

```sql
UPDATE users SET plan_tier = 'EDUCATION' WHERE handle = 'alice';
UPDATE budget_windows SET cap_kind = 'LIMITED', cap_seconds = CASE period
    WHEN 'SESSION' THEN 4 * 3600 ELSE 40 * 3600 END
  WHERE user_id = (SELECT id FROM users WHERE handle = 'alice');
```

### Upgrading from the old web app

This version starts the database schema afresh. Nothing migrates the previous `opendc-web`
database, and the compose keys have changed: start from `.env.example` and the compose files in the
repository root, not from an old `.env`.
