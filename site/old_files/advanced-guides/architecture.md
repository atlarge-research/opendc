---
sidebar_position: 2
---

# Architecture

OpenDC consists of four components: a Kotlin simulator, a SQL database, a Quarkus-based
[API](https://github.com/atlarge-research/opendc/tree/master/opendc-web/opendc-web-api), and a 
React.js [frontend](https://github.com/atlarge-research/opendc/tree/master/opendc-web/opendc-web-api).

![OpenDC Component Diagram](img/component-diagram.png)

On the frontend, users can construct a topology by specifying a datacenter's rooms, racks and machines, and create
scenarios to see how a workload trace runs on that topology. The frontend communicates with the web server via a REST
API over HTTP.

The (Swagger/OpenAPI compliant) API spec specifies what requests the frontend can make to the web server. To view this
specification, go to the [Swagger Editor](https://editor.swagger.io/) and paste in
our [API spec](https://api.opendc.org/q/openapi).

The web server receives API requests and processes them in the database. When the frontend requests to run a new
scenario, the web server adds it to the `scenarios` collection in the database and sets its `state` as `PENDING`.

The simulator monitors the database for `PENDING` scenarios, and simulates them as they are submitted. The results of
the simulations are processed and aggregated in memory. Afterwards, the aggregated summary is written to the database,
which the frontend can then again retrieve via the web server.
