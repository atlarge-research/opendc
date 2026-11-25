---
description: The settings of the OpenDC server, with their defaults.
---

# Configuration

This page lists the settings of the server. [Deployment](deployment.md) explains which of them a given kind of
deployment needs.

## How settings are given

The server is a [Quarkus](https://quarkus.io) application and reads its settings as Quarkus does: from environment
variables, from system properties and from `config/application.properties` in the directory it starts in. The tables
below name each setting by its property key. The corresponding environment variable is the key in upper case, with
every dot and dash replaced by an underscore: `opendc.storage.s3.bucket` becomes `OPENDC_STORAGE_S3_BUCKET`.

Durations use the ISO 8601 notation, such as `PT30S` for 30&nbsp;seconds and `P7D` for 7&nbsp;days.

## Database

The server needs a PostgreSQL database in production. It applies its schema migrations when it starts.

| Environment variable  | Meaning                                                            |
|-----------------------|--------------------------------------------------------------------|
| `OPENDC_DB_URL`       | the JDBC address, such as `jdbc:postgresql://localhost:5432/opendc` |
| `OPENDC_DB_USERNAME`  | the user                                                           |
| `OPENDC_DB_PASSWORD`  | the password                                                       |

## Sign-in

| Key                            | Default  | Meaning                                                                    |
|--------------------------------|----------|----------------------------------------------------------------------------|
| `opendc.auth.mode`             | `auth0`  | `auth0` signs people in; `anonymous` treats every request as one administrator. |
| `quarkus.oidc.tenant-enabled`  | `false`  | Has to be `true` in `auth0` mode.                                          |
| `quarkus.oidc.auth-server-url` |          | The address of the Auth0 tenant, with a trailing slash.                    |
| `quarkus.oidc.token.audience`  |          | The identifier of the Auth0 API.                                           |
| `opendc.auth.auth0.client-id`  |          | The client ID that the web application signs in with.                      |
| `opendc.auth.admins`           |          | The subjects that become administrators when they sign in, separated by commas. |

Removing a subject from `opendc.auth.admins` does not take the administrator role away from an existing account.

## Storage

| Key                                 | Default     | Meaning                                                               |
|-------------------------------------|-------------|-----------------------------------------------------------------------|
| `opendc.storage.kind`               | `local`     | `local` keeps files in a directory; `s3` keeps them in a bucket.      |
| `opendc.storage.directory`          | `data`      | The directory of `local` storage.                                     |
| `opendc.storage.s3.bucket`          |             | The bucket.                                                           |
| `opendc.storage.s3.access-key`      |             | The access key.                                                       |
| `opendc.storage.s3.secret-key`      |             | The secret key.                                                       |
| `opendc.storage.s3.endpoint`        | AWS         | The address of an S3-compatible service other than AWS.               |
| `opendc.storage.s3.public-endpoint` | `endpoint`  | The address at which browsers reach the bucket.                       |
| `opendc.storage.s3.region`          | `us-east-1` | The region.                                                           |
| `opendc.storage.s3.path-style`      | `true`      | Whether the bucket is addressed in the path, not the host name.       |
| `opendc.storage.s3.upload-window`   | `PT1H`      | How long the signed URLs of an upload stay valid.                     |
| `opendc.downloads.signing-key`      | random      | The key that signs download links: at least 32&nbsp;bytes, in Base64. |
| `quarkus.http.limits.max-body-size` | `1G`        | The largest table that an upload through the server may carry.        |

Without `opendc.downloads.signing-key`, each server process generates its own key. Download links then stop working
when the server restarts, and do not work across several servers.

## Telemetry

| Key                     | Default  | Meaning                                                                    |
|-------------------------|----------|----------------------------------------------------------------------------|
| `opendc.telemetry.kind` | `memory` | `memory` keeps progress in the server; `redis` shares it between servers.  |
| `opendc.telemetry.ttl`  | `PT6H`   | How long the last report of a run is kept.                                 |
| `quarkus.redis.hosts`   |          | The address of Redis, such as `redis://redis:6379`.                        |

## Execution

| Key                                    | Default                                  | Meaning                                              |
|----------------------------------------|------------------------------------------|------------------------------------------------------|
| `opendc.execution.dispatcher`          | `local`                                  | `local`, `kubernetes` or `slurm`.                    |
| `opendc.execution.telemetry-url`       | `http://localhost:8080/api/v1/telemetry` | Where launchers report progress. Launchers have to reach it. |
| `opendc.execution.url-lifetime`        | `P7D`                                    | How long the signed URLs of an execution stay valid. At most 7&nbsp;days. |
| `opendc.execution.launch-retry-delay`  | `PT30S`                                  | How long the server waits after the platform was unavailable. |
| `opendc.dispatcher.launcher-lib`       | `launcher/lib` in the distribution       | The directory with the launcher's JAR files.         |

### Estimates, packing and retries

These settings tune how the server [sizes executions](execution.md#estimating-a-run). The defaults suit most
deployments.

| Key                                                              | Default  | Meaning                                              |
|------------------------------------------------------------------|----------|------------------------------------------------------|
| `opendc.execution.estimator.base-memory-mb`                      | `80`     | memory of any run, in MiB                            |
| `opendc.execution.estimator.memory-per-host-mb`                  | `0.03`   | memory per host                                      |
| `opendc.execution.estimator.memory-per-million-fragments-mb`     | `80`     | memory per million fragments                         |
| `opendc.execution.estimator.memory-per-million-tasks-mb`         | `2500`   | memory per million tasks                             |
| `opendc.execution.estimator.base-seconds`                        | `2`      | CPU time of any run, in seconds                      |
| `opendc.execution.estimator.load-seconds-per-million-fragments`  | `1.2`    | time to load a million fragments                     |
| `opendc.execution.estimator.simulate-seconds-per-million-fragments` | `1.6` | time to simulate a million fragments                 |
| `opendc.execution.estimator.seconds-per-million-tasks`           | `1800`   | time per million tasks                               |
| `opendc.execution.estimator.export-cost-per-host`                | `0.005`  | relative cost of exporting the host file, per host   |
| `opendc.execution.estimator.failure-overhead`                    | `0.25`   | relative cost of a failure model                     |
| `opendc.execution.estimator.checkpoint-overhead`                 | `0.15`   | relative cost of a checkpoint model                  |
| `opendc.execution.estimator.runtime-multiplier`                  | `1.0`    | scales every time estimate of this deployment        |
| `opendc.execution.estimator.memory-multiplier`                   | `1.0`    | scales every memory estimate of this deployment      |
| `opendc.execution.packing.jvm-baseline-mb`                       | `256`    | memory of the launcher itself, in MiB                |
| `opendc.execution.packing.heap-headroom`                         | `1.0`    | factor on the estimated memory of a run              |
| `opendc.execution.packing.startup-seconds`                       | `30`     | start-up allowance in the time limit                 |
| `opendc.execution.packing.time-safety-factor`                    | `3.0`    | factor on the estimated time in the time limit       |
| `opendc.execution.retry.max-attempts`                            | `3`      | attempts per run, the first included                 |
| `opendc.execution.retry.growth-factor`                           | `2.0`    | growth of memory or time on a retry                  |
| `opendc.execution.retry.max-memory-request-mb`                   | `32768`  | the largest memory request of a retry, in MiB        |

The two multipliers accept values from 0.25 to 4. They adapt the estimates to the speed of a platform without changing
what an experiment costs its submitter.

## Dispatchers

Each dispatcher reads its settings under its own prefix.

### Local dispatcher

| Key                                 | Default                        | Meaning                                    |
|-------------------------------------|--------------------------------|--------------------------------------------|
| `opendc.dispatcher.local.cores`     | every core of the machine      | the cores that executions share            |
| `opendc.dispatcher.local.memory-mb` | half the memory of the machine | the memory that executions share, in MiB   |
| `opendc.dispatcher.local.work-dir`  | `data/executions`              | the directory in which launchers work      |

### Kubernetes dispatcher

| Key                                            | Default                                           | Meaning                                         |
|------------------------------------------------|---------------------------------------------------|-------------------------------------------------|
| `opendc.dispatcher.kubernetes.namespace`       | the namespace of the server                       | where Jobs are created                          |
| `opendc.dispatcher.kubernetes.image`           | `ghcr.io/atlarge-research/opendc-launcher:latest` | the launcher image                              |
| `opendc.dispatcher.kubernetes.image-pull-policy` | `if-not-present`                                | when the image is pulled                        |
| `opendc.dispatcher.kubernetes.service-account` | `default`                                         | the service account of execution pods           |
| `opendc.dispatcher.kubernetes.priority-class`  |                                                   | the priority class of execution pods            |
| `opendc.dispatcher.kubernetes.slot-cores`      | `4`                                               | the cores of one execution                      |
| `opendc.dispatcher.kubernetes.slot-memory-mb`  | `8192`                                            | the memory of one execution, in MiB             |
| `opendc.dispatcher.kubernetes.max-concurrent`  | `16`                                              | the executions that may run at once             |
| `opendc.dispatcher.kubernetes.scratch-size-mb` | `20480`                                           | the scratch space of a pod, in MiB              |
| `opendc.dispatcher.kubernetes.ttl`             | `PT168H`                                          | how long a finished Job is kept                 |
| `opendc.dispatcher.kubernetes.pending-timeout` | `PT1H`                                            | how long a pod may wait for a node              |

### SLURM dispatcher

| Key                                       | Default              | Meaning                                                      |
|-------------------------------------------|----------------------|--------------------------------------------------------------|
| `opendc.dispatcher.slurm.host`            | required             | the head node                                                |
| `opendc.dispatcher.slurm.port`            | `22`                 | its SSH port                                                 |
| `opendc.dispatcher.slurm.user`            | required             | the user on the cluster                                      |
| `opendc.dispatcher.slurm.identity-file`   | required             | the private key                                              |
| `opendc.dispatcher.slurm.known-hosts`     | `~/.ssh/known_hosts` | the host keys the server trusts                              |
| `opendc.dispatcher.slurm.jump-host`       |                      | a host to connect through, as `user@host`                    |
| `opendc.dispatcher.slurm.remote-root`     | `opendc`             | the directory on the shared file system                      |
| `opendc.dispatcher.slurm.java`            | `java`               | the Java executable on the cluster                           |
| `opendc.dispatcher.slurm.partition`       |                      | the partition                                                |
| `opendc.dispatcher.slurm.sbatch-options`  |                      | further options for `sbatch`                                 |
| `opendc.dispatcher.slurm.slot-cores`      | required             | the cores of one execution                                   |
| `opendc.dispatcher.slurm.slot-memory-mb`  | required             | the memory of one execution, in MiB                          |
| `opendc.dispatcher.slurm.time-cap`        |                      | the longest time limit of a job                              |
| `opendc.dispatcher.slurm.max-jobs`        | `4`                  | the jobs that may be queued or running at once               |
| `opendc.dispatcher.slurm.pending-timeout` | `PT1H`               | how long a job may wait in the queue                         |
| `opendc.dispatcher.slurm.poll-interval`   | `PT15S`              | how often the server asks the cluster for the state of jobs  |
| `opendc.dispatcher.slurm.cache-retention` | `P7D`                | how long staged traces stay on the cluster                   |

## Trace imports

| Key                                        | Default       | Meaning                                                   |
|--------------------------------------------|---------------|-----------------------------------------------------------|
| `opendc.traces.import.max-bytes`           | `10737418240` | the largest table an import may fetch: 10&nbsp;GiB        |
| `opendc.traces.import.deadline`            | `PT6H`        | how long an import may take                               |
| `opendc.traces.import.concurrency`         | `2`           | the imports that run at once                              |
| `opendc.traces.import.allow-private-hosts` | `false`       | whether imports may fetch from private addresses          |

## Web server

| Key                           | Default                         | Meaning                                                   |
|-------------------------------|---------------------------------|-----------------------------------------------------------|
| `quarkus.http.port`           | `8080`                          | the port of the API and the web application               |
| `quarkus.management.port`     | `9000`                          | the port of health checks, metrics and the OpenAPI description |
| `opendc.frontend.directory`   | `frontend` in the distribution  | the files of the web application; unset, only the API is served |
| `quarkus.http.cors.enabled`   | `false`                         | whether another origin may call the API                   |
| `quarkus.http.cors.origins`   |                                 | the origins that may call the API                         |
| `OPENDC_SERVER_OPTS`          | `-XX:MaxRAMPercentage=25` in the image | JVM options of the server                          |
