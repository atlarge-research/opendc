---
description: The REST API of the OpenDC server, with its authentication, error format and endpoints.
---

# REST API

The server exposes everything the web application does through a REST API under `/api/v1`. This page describes how a
script authenticates, how the server reports errors, and which endpoints exist. Most scripts do not need the API
directly: the [command-line interface](../reference/cli.md#running-on-a-server) and the
[Python SDK](../python-sdk/server.md) submit experiments and download results through it.

## A first session

The following session creates a project, submits the experiment in `experiment.json` and downloads its results. It
uses [curl](https://curl.se) and [jq](https://jqlang.github.io/jq/), and a deployment at `http://localhost:8080` that
signs nobody in.

``` bash
api=http://localhost:8080/api/v1

project=$(curl -fsS -X POST "$api/projects" -H 'Content-Type: application/json' \
    -d '{"name": "Scripted"}' | jq -r '.id')

experiment=$(jq -n --arg project "$project" --slurpfile spec experiment.json \
    '{projectId: $project, name: "First run", spec: $spec[0]}' |
    curl -fsS -X POST "$api/experiments" -H 'Content-Type: application/json' -d @- | jq -r '.id')

curl -fsS -X POST "$api/experiments/$experiment/submit"
curl -fsS "$api/experiments/$experiment/status" | jq '{state, completedTasks, totalTasks}'

link=$(curl -fsS -X POST "$api/experiments/$experiment/archive/link" | jq -r '.url')
curl -fsS -o results.zip "http://localhost:8080/$link"
```

The body of `POST /experiments` wraps an [experiment document](../reference/experiment/index.md) in `spec`. The workload
of the document has to reference a trace by its name in the [trace library](using.md#traces), such as
`{ "type": "named", "name": "bitbrains-small" }`. The status request is repeated until `state` is one of `succeeded`,
`partial`, `failed` or `cancelled`.

## Authentication

A deployment in anonymous mode accepts requests without credentials. Any other deployment expects a bearer token in the
`Authorization` header:

``` bash
curl -H "Authorization: Bearer $OPENDC_TOKEN" "$api/projects"
```

A script uses a *personal access token*, which starts with `odc_pat_`. An account creates tokens in the
[account menu](using.md#account) or with `POST /me/tokens`. The server shows a token once and stores only its hash, so
a lost token has to be replaced. `GET /config` needs no credentials and tells a client which mode the deployment uses.

## Errors

The server answers a failed request with a JSON *problem document*. Submitting an experiment whose cluster has no hosts
and whose workload names an unknown trace returns:

``` json
{
    "status": 400,
    "title": "The experiment document is invalid",
    "issues": [
        { "path": "topologies[0].datacenters[0].clusters[0].hosts", "message": "must not be empty" },
        { "path": "workloads[0].source", "message": "no trace called nope in your library" }
    ]
}
```

`issues` names each problem with the path of the field it concerns, in the notation of
[validation issues](../kotlin-sdk/model.md#validation). A problem without issues, such as a missing resource, has only
`status` and `title`. The status codes have fixed meanings:

| Status | Meaning                                                                                      |
|--------|----------------------------------------------------------------------------------------------|
| 400    | The request or the document in it is invalid.                                                |
| 401    | The request has no valid credentials.                                                        |
| 403    | The account may not do this, has not chosen a handle yet, or is deactivated.                 |
| 404    | The resource does not exist, or the account is not allowed to see it.                        |
| 409    | The request conflicts with the state of the resource, such as editing a submitted experiment. |

## Endpoints

The tables below list the endpoints by resource. Paths are relative to `/api/v1`. The last column states who may call
an endpoint: any signed-in account, a member of the project with at least the given
[role](using.md#projects), the owner of a trace, or an administrator.

### Account

| Method and path           | Purpose                                             | Allowed for |
|---------------------------|-----------------------------------------------------|-------------|
| `GET /config`             | The sign-in settings of the deployment.             | everyone    |
| `GET /me`                 | The profile, plan and budgets of the account.       | account     |
| `PUT /me/profile`         | Sets the handle and the display name.               | account     |
| `DELETE /me`              | Deactivates the account.                            | account     |
| `GET /me/tokens`          | Lists the personal access tokens.                   | account     |
| `POST /me/tokens`         | Creates a token and returns its secret once.        | account     |
| `DELETE /me/tokens/{id}`  | Revokes a token.                                    | account     |

### Projects and topologies

| Method and path                          | Purpose                                                  | Allowed for    |
|------------------------------------------|----------------------------------------------------------|----------------|
| `GET /projects`                          | Lists projects. `filter` is `all`, `own` or `shared`; `q` searches by name. | account |
| `POST /projects`                         | Creates a project, owned by the caller.                  | account        |
| `GET /projects/{id}`                     | Reads a project.                                         | viewer         |
| `PATCH /projects/{id}`                   | Renames a project.                                       | editor         |
| `DELETE /projects/{id}`                  | Deletes a project and stops its running experiments.     | owner          |
| `GET /projects/{id}/members`             | Lists the members.                                       | viewer         |
| `POST /projects/{id}/members`            | Adds a member by `handle` with a `role`.                 | owner          |
| `PATCH /projects/{id}/members/{handle}`  | Changes a role.                                          | owner          |
| `DELETE /projects/{id}/members/{handle}` | Removes a member. Every member may remove themselves.    | owner          |
| `GET /topologies?project={id}`           | Lists the topologies of a project.                       | viewer         |
| `POST /topologies`                       | Creates a topology.                                      | editor         |
| `GET /topologies/{id}`                   | Reads a topology.                                        | viewer         |
| `PUT /topologies/{id}`                   | Replaces a topology.                                     | editor         |
| `DELETE /topologies/{id}`                | Deletes a topology.                                      | editor         |
| `GET /catalogs`, `GET /catalogs/{name}`  | The options the editors offer, such as schedulers and host templates. | account |

### Experiments

| Method and path                                   | Purpose                                                          | Allowed for |
|---------------------------------------------------|------------------------------------------------------------------|-------------|
| `GET /experiments`                                | Lists experiments, newest first. `project` selects a project; `limit` and `offset` page. | account |
| `POST /experiments`                               | Creates a draft from `projectId`, `name` and `spec`.             | editor      |
| `POST /experiments/preview`                       | Counts the scenarios of a document, estimates it and lists its issues. | account |
| `GET /experiments/{id}`                           | Reads an experiment with its document.                           | viewer      |
| `PUT /experiments/{id}`                           | Replaces a draft.                                                | editor      |
| `DELETE /experiments/{id}`                        | Deletes an experiment and its results.                           | editor      |
| `POST /experiments/{id}/submit`                   | Checks a draft, freezes it and queues its runs.                  | editor      |
| `POST /experiments/{id}/cancel`                   | Cancels the runs that have not ended.                            | editor      |
| `POST /experiments/{id}/clone`                    | Creates a draft with the same document.                          | editor      |
| `GET /experiments/{id}/status`                    | The state and progress of the experiment and of each scenario.   | viewer      |
| `GET /experiments/{id}/scenarios/{index}`         | The runs of a scenario, with their attempts and output files.    | viewer      |
| `GET /experiments/{id}/scenarios/{index}/logs`    | The log of a scenario, as plain text.                            | viewer      |
| `POST /experiments/{id}/scenarios/{index}/retry`  | Runs a finished scenario again.                                  | editor      |
| `GET /experiments/{id}/results`                   | The chart series of each scenario. `buckets` sets the number of points. | viewer |
| `GET /experiments/{id}/archive`                   | A zip archive with the output files of the successful runs.      | viewer      |
| `POST /experiments/{id}/archive/link`             | A link to the archive that works for 5&nbsp;minutes without credentials. | viewer |

### Traces

| Method and path                         | Purpose                                                         | Allowed for |
|-----------------------------------------|-----------------------------------------------------------------|-------------|
| `GET /traces`                           | Lists the trace library. `kind` is `workload`, `carbon` or `failure`. | account |
| `GET /traces/kinds`                     | The tables of each kind of trace.                               | account     |
| `POST /traces`                          | Registers an upload and returns where to send each table.       | account     |
| `PUT /traces/{id}/tables/{table}`       | Uploads a table through the server.                             | trace owner |
| `POST /traces/{id}/complete`            | Checks the uploaded tables and adds the trace to the library.   | trace owner |
| `GET /traces/{id}`                      | Reads a trace.                                                  | account with access |
| `PATCH /traces/{id}`                    | Changes the name or description.                                | trace owner |
| `DELETE /traces/{id}`                   | Deletes a trace.                                                | trace owner |
| `GET /traces/{id}/content`              | A zip archive of the tables.                                    | account with access |
| `POST /traces/{id}/content/link`        | A link to that archive that works for 5&nbsp;minutes.           | account with access |
| `GET /traces/{id}/shares`               | Lists the accounts the trace is shared with.                    | trace owner |
| `POST /traces/{id}/shares`              | Shares the trace with a `handle`.                               | trace owner |
| `DELETE /traces/{id}/shares/{handle}`   | Ends a share.                                                   | trace owner |
| `GET /traces/imports`                   | Lists the imports of the account.                               | account     |
| `POST /traces/imports`                  | Starts an import from one URL per table.                        | account     |
| `DELETE /traces/imports/{id}`           | Dismisses a finished import.                                    | account     |
| `GET /downloads/{ticket}`               | Follows a link created by one of the `link` endpoints.          | everyone    |

An upload takes three requests. `POST /traces` states the kind, the name and the size of each table, and answers with
one upload target per table. A deployment with S3 storage hands out signed URLs that take the bytes directly, in parts
for a large table. A deployment with local storage hands out a `PUT /traces/{id}/tables/{table}` path instead. `POST
/traces/{id}/complete` then checks that every table arrived and is a Parquet file with the columns of its kind.

### Administration

| Method and path                      | Purpose                                                       | Allowed for   |
|--------------------------------------|---------------------------------------------------------------|---------------|
| `GET /admin/executions`              | Lists executions. `state` filters; the default is the live ones. | administrator |
| `GET /admin/executions/{id}`         | An execution with its runs.                                   | administrator |
| `GET /admin/executions/{id}/logs`    | The stored launcher log.                                      | administrator |
| `POST /admin/executions/{id}/retry`  | Runs the failed runs of an execution again.                   | administrator |
| `GET /admin/capacity`                | The capacity of the platform and the slot of the dispatcher.  | administrator |
| `GET /admin/users`                   | Lists accounts.                                               | administrator |

`POST /telemetry` is reserved for launchers, which authenticate with a token that the server creates for each
[execution](execution.md#the-launcher).

## OpenAPI description

The server generates an OpenAPI description of the API and a Swagger UI to explore it. Both live on the management
port, which is 9000 by default and is not meant to be public: the description at `/q/openapi` and the Swagger UI at
`/q/docs`.
