---
description: How to monitor and administer a running OpenDC deployment.
---

# Operations

This page is for the people who keep a deployment running. It covers monitoring, the admin panel, the simulation budget
of accounts, the safety of trace imports, and where the server keeps its data.

## Health and metrics

The server exposes health checks and metrics on a separate *management port*, which is 9000 by default. An ingress
should not route this port.

| Path                 | Answers                                                       |
|----------------------|---------------------------------------------------------------|
| `/q/health/started`  | whether the server has started                                |
| `/q/health/live`     | whether the server process is alive                           |
| `/q/health/ready`    | whether the server can serve requests, database included      |
| `/q/metrics`         | metrics in the Prometheus format                              |

Besides the metrics of the JVM and the request timings of the API, the server exports metrics about executions:

| Metric                              | Labels                                                    | Meaning                                         |
|-------------------------------------|-----------------------------------------------------------|-------------------------------------------------|
| `opendc_executions_launched_total`  | `outcome`: `accepted`, `rejected`, `unavailable`          | executions handed to the platform               |
| `opendc_executions_settled_total`   | `reason`: an [exit reason](execution.md#settling-and-retrying) | executions that ended, by cause            |
| `opendc_executions`                 | `state`: `queued`, `submitted`, `running`, `succeeded`, `failed`, `cancelled` | executions in each state    |
| `opendc_units_queued`               |                                                           | runs that wait for an execution                 |
| `opendc_platform_cores_total`       |                                                           | the cores of the platform                       |
| `opendc_platform_cores_allocated`   |                                                           | the cores that executions hold                  |

The server reads the last four metrics from the database every 15&nbsp;seconds. Every server of a deployment therefore
reports the same numbers, and a dashboard has to take their maximum, not their sum.

`deploy/grafana/opendc.json` is a [Grafana](https://grafana.com) dashboard of these metrics, together with API traffic,
latency and heap usage. It works with any Prometheus data source that scrapes the management port. A test of the server
checks that every query of the dashboard names a metric and labels that the server exports.

## The admin panel

Administrators are the accounts named in `OPENDC_AUTH_ADMINS`, and everybody in anonymous mode. The account menu of an
administrator opens the admin panel, which has three tabs:

**Executions**
:   lists the executions of all accounts, by default the ones that have not ended. An execution shows its cores, memory
    and time limit, the reason it ended, its runs with their estimates and outcomes, and the launcher log. The panel can
    start a further attempt for the failed runs of an execution, beyond the automatic retries. This suits a failure
    that is known to be transient.

**Capacity**
:   shows how many cores and how much memory of the platform are in use, and the slot that the dispatcher gives one
    execution.

**Accounts**
:   lists the accounts with their plans and the number of their projects.

Administrators cannot see the projects of other accounts.

## Plans and budgets

Every account has a *plan*, which limits its simulation time in two windows: a session of 5&nbsp;hours and a week.

| Plan       | Per session | Per week  |
|------------|-------------|-----------|
| free       | 1&nbsp;h    | 10&nbsp;h |
| education  | 4&nbsp;h    | 40&nbsp;h |
| enterprise | unlimited   | unlimited |

Simulation time is processor time on the platform, not simulated time. The server handles it in three steps:

1. When an account submits an experiment, the server estimates the CPU time of every run. It refuses the experiment if
   the estimate exceeds what is left in either window.
2. While runs wait or simulate, their estimates stay reserved.
3. When an execution ends, the server charges the submitter the cores of the execution multiplied by the time it ran.
   Executions that ended for lack of resources are free, as [Settling and retrying](execution.md#settling-and-retrying)
   lists.

The server never stops a running simulation because a budget ran out. The account of anonymous mode has no windows and
is not limited.

A plan changes in the database only. A window copies the limits of its plan when it opens, so a change has to update
both:

``` sql
UPDATE users SET plan_tier = 'EDUCATION' WHERE handle = 'alice';
UPDATE budget_windows SET cap_kind = 'LIMITED', cap_seconds = CASE period
    WHEN 'SESSION' THEN 4 * 3600 ELSE 40 * 3600 END
  WHERE user_id = (SELECT id FROM users WHERE handle = 'alice');
```

## Trace imports

An account can [import a trace](using.md#traces) from public addresses, which the server fetches itself. Fetching
addresses on behalf of users is a risk, because the server may reach machines that its users cannot. The server
therefore applies these rules:

- It accepts only `http` and `https` addresses.
- It refuses an address that resolves to a private, loopback or link-local address, and checks every redirect in the
  same way.
- It gives up on a table larger than 10&nbsp;GiB, on a remote server that stays silent for 2&nbsp;minutes, and on an
  import that takes longer than 6&nbsp;hours.

One gap remains. The server checks a host name before each request but resolves it again to connect, so a host whose
address changes in between passes the check. Where the server can reach sensitive internal services, the network
should restrict its outbound traffic as well.

A deployment on an intranet that imports from its own mirrors sets `OPENDC_TRACES_IMPORT_ALLOW_PRIVATE_HOSTS=true`. The
limits are [settings](configuration.md#trace-imports).

## Where the data lives

The database holds everything except files: accounts, projects, topologies, experiment documents, the state of every
run and execution, and budgets. The object store holds the files under these keys:

| Key                                                              | Content                                   |
|------------------------------------------------------------------|-------------------------------------------|
| `traces/<trace>/<table>.parquet`                                 | one table of a trace                      |
| `experiments/<experiment>/results/<scenario>/seed=<seed>/<file>` | one output file of a run                  |
| `experiments/<experiment>/executions/<execution>/launcher.log`   | the end of the log of an execution        |
| `experiments/<experiment>/executions/<execution>/units/...`      | the outcome of each run of an execution   |

Deleting an experiment deletes everything under its key. The telemetry store holds only the progress of running
simulations and needs no backup.

A deployment of several servers has to share three things: the database, an S3 bucket and Redis. It also has to set
`opendc.downloads.signing-key`, so that a download link created by one server works on another.
