# Deploying OpenDC

The server distribution is the whole self-hosted product: the API, the frontend it serves from the
same origin, and the launcher its local dispatcher runs simulations with. Where simulations run is
one setting, `OPENDC_EXECUTION_DISPATCHER`: `local` (beside the server), `kubernetes` (one Job per
execution) or `slurm` (one batch job per execution, over SSH).

- [Run it from a checkout](#run-it-from-a-checkout) with Docker Compose
- [Sign people in](#sign-people-in) through Auth0
- [Run on Kubernetes](#run-on-kubernetes)
- [Run on a SLURM cluster](#run-on-a-slurm-cluster), such as DAS-5
- [Operate it](#operate-it): health, metrics, administrators and plans

## Run it from a checkout

```sh
./gradlew :opendc-web:opendc-web-server:installDist
cp .env.example .env                  # set OPENDC_DB_PASSWORD
docker compose up -d --build          # docker-compose.override.yml builds the image
deploy/smoke-test.sh                  # runs an experiment end to end
```

Then open <http://localhost:8080>. The stack signs nobody in (`OPENDC_AUTH_MODE=anonymous`), keeps
traces and results on a volume, and runs simulations inside the server container, held to
`OPENDC_MEMORY_LIMIT` (4 GB) of which launchers share `OPENDC_SIMULATION_MEMORY_MB` (2 GB).

To keep traces and results in a bucket and live progress in Redis instead, add the S3 overlay, which
brings up MinIO and Redis (set `MINIO_ROOT_PASSWORD` in `.env`):

```sh
docker compose -f docker-compose.yml -f docker-compose.override.yml -f docker-compose.s3.yml up -d
```

`docker compose down -v` stops everything and deletes its data.

### Without Docker

The distribution under `opendc-web/opendc-web-server/build/install/opendc-server` starts with
`bin/opendc-server` from that directory, whose `config/` points the server at the frontend and the
launcher beside it. It needs a PostgreSQL database named in `OPENDC_DB_URL`, `OPENDC_DB_USERNAME`
and `OPENDC_DB_PASSWORD`. CI runs exactly this against the runner's PostgreSQL on every build.

For development, run `./gradlew :opendc-web:opendc-web-server:quarkusDev` (H2, anonymous) and
`pnpm dev` in `opendc-web/opendc-web-frontend`, then open <http://localhost:3000>.

### Images

Both images package a distribution Gradle has already built, so building them needs no network:

```sh
./gradlew :opendc-web:opendc-web-server:installDist :opendc-web:opendc-web-launcher:installDist
docker build -f opendc-web/opendc-web-server/Dockerfile -t opendc-server .
docker build -f opendc-web/opendc-web-launcher/Dockerfile -t opendc-launcher .
```

The launcher image is what the Kubernetes dispatcher runs, one Job per execution; it has to be the
same version as the server, because the manifest it is handed is their contract.

### Hosting the frontend elsewhere

Build the export with `NEXT_PUBLIC_API_BASE_URL=https://api.example.org pnpm build`, host `out/`
with a fallback from `/page` to `/page.html` (nginx: `try_files $uri $uri.html $uri/ =404`), and let
the server accept the other origin with `QUARKUS_HTTP_CORS_ENABLED=true` and
`QUARKUS_HTTP_CORS_ORIGINS=https://app.example.org`.

## Sign people in

A deployment either signs nobody in (`anonymous`: every request is one implicit administrator, for a
single person running OpenDC for themselves) or signs people in through Auth0 (`auth0`, the default
when nothing is set). The frontend reads which one from `GET /api/v1/config` at runtime, so the same
build serves both.

In Auth0, create:

1. an **API** whose identifier is the audience the server checks, e.g. `https://opendc.example.org/api`;
2. a **Single Page Application** with the deployment's URL as an allowed callback, logout and web
   origin, refresh token rotation on, and access to that API.

Then give the server:

| Variable | Value |
|---|---|
| `OPENDC_AUTH_MODE` | `auth0` |
| `QUARKUS_OIDC_TENANT_ENABLED` | `true` |
| `QUARKUS_OIDC_AUTH_SERVER_URL` | `https://<tenant>.auth0.com/`, with the trailing slash |
| `QUARKUS_OIDC_TOKEN_AUDIENCE` | the API identifier |
| `OPENDC_AUTH_AUTH0_CLIENT_ID` | the SPA's client id, which the frontend signs in with |
| `OPENDC_AUTH_ADMINS` | comma-separated Auth0 subjects (`auth0\|...`) of the first administrators |

The server refuses to start when the mode and the OIDC tenant disagree. With Compose,
`docker-compose.prod.yml` maps these from `OPENDC_AUTH0_DOMAIN`, `OPENDC_AUTH0_AUDIENCE`,
`OPENDC_AUTH0_CLIENT_ID` and `OPENDC_AUTH_ADMINS` in `.env`.

An account is created the first time someone signs in, and picks its handle once; traces are named
after it (`alice/nightly`). From the account menu people mint **personal access tokens**
(`odc_pat_...`), which work in either mode and are what scripts and the CLI use:

```sh
OPENDC_TOKEN=odc_pat_... opendc run experiment.json --api-url https://opendc.example.org
```

## Run on Kubernetes

`deploy/kubernetes` is a kustomization: the server, the RBAC its dispatcher needs, the account
execution pods run as, a NetworkPolicy closing those pods to inbound traffic, an ephemeral Redis for
live progress, and an ingress. It expects a PostgreSQL database and an S3-compatible bucket to exist.

1. Copy `secrets.example.env` to `secrets.env` and fill in the database, the bucket's credentials and
   a download signing key (`openssl rand -base64 32`).
2. Edit `server.env`: the bucket's endpoint as pods reach it and as browsers reach it, the Auth0
   settings above, the launcher image, and the shape of one execution (`SLOT_CORES`,
   `SLOT_MEMORY_MB`, which should fit one node) and how many may run at once.
3. Edit `ingress.yaml` for your host, ingress class and TLS secret, and pin the server image tag in
   `kustomization.yaml` to the same version as the launcher image.
4. `kubectl apply -k deploy/kubernetes`, then watch `kubectl -n opendc get jobs,pods` while an
   experiment runs.

The bucket has to allow the deployment's origin to `PUT` and `GET` (browsers upload traces and
download results straight from it), and it has to be reachable from execution pods, which read their
inputs and write their outputs through signed URLs and need no credentials of their own.

The server refuses to start on Kubernetes with local storage or a telemetry URL pods cannot reach,
since executions would have nowhere to read from or report to. Without the optional node reader in
`rbac.yaml`, the admin panel reports the dispatcher's own ceiling instead of the cluster's capacity.

## Run on a SLURM cluster

The SLURM dispatcher submits each execution as a batch job over SSH. Compute nodes are assumed to
reach nothing but their shared filesystem, so the server stages the launcher, the traces and a
rewritten manifest there over SFTP, and copies each finished run back itself. For the same reason a
SLURM run shows no live progress, only its outcome. Run a single server replica.

The `das5` profile has the settings for DAS-5, reached through the VU bastion:

```sh
OPENDC_SLURM_USER=<das5 user> OPENDC_VUNET_ID=<vunet id> \
OPENDC_SLURM_IDENTITY_FILE=/run/secrets/das5_key \
OPENDC_SLURM_KNOWN_HOSTS=/run/secrets/known_hosts \
QUARKUS_PROFILE=prod,das5 bin/opendc-server
```

Before the first run:

- put an unencrypted key for both the bastion and the head node in the identity file, readable by
  the server only;
- put both host keys in the known-hosts file: nothing else is trusted, and a host the file does not
  vouch for is refused;
- install a Java 21 runtime at `/var/scratch/<user>/opendc/jdk-21` on the shared filesystem; the
  server checks the version before it uses it.

Jobs are held to the 15-minute daytime limit on 16-core nodes. The node memory in the profile
(56 GB) is an assumption to check with `sinfo -o "%n %m"` before relying on it. Another cluster
takes the same keys under `opendc.dispatcher.slurm.*` (`host`, `user`, `jump-host`, `remote-root`,
`java`, `partition`, `sbatch-options`, `slot-cores`, `slot-memory-mb`, `time-cap`, `max-jobs`).

## Operate it

### Health and metrics

Health checks and Prometheus metrics are on a management port of their own, 9000, which is never
routed by the ingress: `/q/health/live`, `/q/health/ready`, `/q/health/started` and `/q/metrics`.
Beside the JVM's and the API's request timings, the server exports:

| Metric | Labels | Meaning |
|---|---|---|
| `opendc_executions_launched_total` | `outcome`: accepted, rejected, unavailable | executions handed to the platform |
| `opendc_executions_settled_total` | `reason`: ok, simulation_error, oom, timeout, walltime, cancelled, rejected, unknown, invalid_spec | executions that ended, by why |
| `opendc_executions` | `state`: queued, submitted, running, succeeded, failed, cancelled | executions in each state |
| `opendc_units_queued` | | runs waiting for an execution |
| `opendc_platform_cores_total`, `opendc_platform_cores_allocated` | | the platform's capacity and what is in use |

The gauges are read from the database every 15 seconds, so every replica reports the same numbers;
take the maximum across replicas, not the sum.

`deploy/grafana/opendc.json` is a dashboard of all of the above plus API traffic, latency and heap:
import it into Grafana and pick the Prometheus data source that scrapes port 9000. The server's
test suite checks that every query in it names a metric and labels the server exports.

### Trace imports

People can import a trace from public URLs, one per table; the server fetches them itself. It
refuses URLs that resolve to private, loopback, link-local or other non-public addresses, checks
every redirect the same way, and gives up on a table larger than
`OPENDC_TRACES_IMPORT_MAX_BYTES` (10 GiB), a server that goes quiet for two minutes, or an import
older than `OPENDC_TRACES_IMPORT_DEADLINE` (6 h). A host is checked before each request and resolved
again when connecting, so a name whose DNS answer changes in between is not caught: where the server
can reach sensitive internal services, restrict its egress at the network as well. An intranet
deployment that imports from its own mirrors sets `OPENDC_TRACES_IMPORT_ALLOW_PRIVATE_HOSTS=true`.

### Administrators and plans

Administrators (those in `OPENDC_AUTH_ADMINS`, and everybody in anonymous mode) get an admin panel
from the account menu: executions on and off the platform with their units and launcher logs, the
platform's capacity, and the accounts. A failed execution can be run again from there past the
automatic retry limit, for a failure the operator knows was transient. Administrators get no access
to other people's projects.

Every account has a plan, which caps its simulation time per 5-hour session and per week: free
1 h / 10 h, education 4 h / 40 h, enterprise unlimited. Plans change in the database. A plan's caps
are copied into an account's budget windows when they open, so change both:

```sql
UPDATE users SET plan_tier = 'EDUCATION' WHERE handle = 'alice';
UPDATE budget_windows SET cap_kind = 'LIMITED', cap_seconds = CASE period
    WHEN 'SESSION' THEN 4 * 3600 ELSE 40 * 3600 END
  WHERE user_id = (SELECT id FROM users WHERE handle = 'alice');
```

### Upgrading from the old web app

The database schema starts afresh with this version: there is no migration from the previous
`opendc-web` deployment's database, and its compose keys differ. Start from `.env.example` and the
compose files in the repository root rather than an old `.env`.
