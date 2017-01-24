# OpenDC

## Architecture

OpenDC consists of four components: a C++ simulator, a SQLite database, a Python Flask web server, and a TypeScript frontend.

On the frontend, users can construct a topology by specifying a datacenter's rooms, racks and machines, and create experiments to see how a workload trace runs on that topology. The frontend communicates with the web server over SocketIO, through a custom REST request/response layer. For example, the frontend might make a `GET` request to `/api/v1/users/{userId}`, but this request is completed via SocketIO, not plane HTTP requests.

The (Swagger/ OpenAPI compliant) API specification specifies what requests the frontend can make to the web server. To view this specification, go to the [Swagger UI](http://petstore.swagger.io/) and "Explore" [opendc-api-spec.json](https://raw.githubusercontent.com/tudelft-atlarge/opendc/master/opendc-api-spec.json).

The web server receives these API requests and processes them. When the frontend requests to run a new experiment, the web server adds a row to the `queued_experiments` table in the SQLite database.

The simulator monitors this `queued_experiments` table, and simulates experiments as they are submitted. It writes the resulting `machine_states` and `task_states` to the database, which the frontend can then again retrieve via the web server.

## Setup

To build and run the full OpenDC stack locally:

First, clone this repository and its submodules:

```bash
git clone --recursive git@github.com:tudelft-atlarge/opendc.git
```

Then... [TO DO]
