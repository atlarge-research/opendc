# Architecture

OpenDC consists of four components: a Kotlin simulator, a MongoDB database, a Python
Flask [API](/opendc-web/opendc-web-api), and a React.js [frontend](/opendc-web/opendc-web-ui).

<p align="center">
    <img src="https://raw.githubusercontent.com/atlarge-research/opendc/master/misc/artwork/opendc-component-diagram.png" alt="OpenDC Component Diagram">
</p>

On the frontend, users can construct a topology by specifying a datacenter's rooms, racks and machines, and create
scenarios to see how a workload trace runs on that topology. The frontend communicates with the web server over
SocketIO, through a custom REST request/response layer. For example, the frontend might make a `GET` request
to `/api/v1/users/{userId}`, but this request is completed via SocketIO, not plain HTTP requests. Note that the API
itself can also be accessed by HTTP.

The (Swagger/OpenAPI compliant) API spec specifies what requests the frontend can make to the web server. To view this
specification, go to the [Swagger Editor](https://editor.swagger.io/) and paste in
our [opendc-api-spec.yml](../opendc-api-spec.yml).

The web server receives API requests and processes them in the database. When the frontend requests to run a new
scenario, the web server adds it to the `scenarios` collection in the database and sets its `state` as `QUEUED`.

The simulator monitors the database for `QUEUED` scenarios, and simulates them as they are submitted. The results of the
simulations are processed and aggregated in memory. Afterwards, the aggregated summary is written to the database, which
the frontend can then again retrieve via the web server.
