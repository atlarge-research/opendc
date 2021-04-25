<h1 align="center">
    <img src="https://raw.githubusercontent.com/atlarge-research/opendc/master/misc/artwork/logo.png" width="100" alt="OpenDC">
    <br>
    OpenDC
</h1>
<p align="center">
    Collaborative Datacenter Simulation and Exploration for Everybody
</p>

<br>

OpenDC is an open-source simulator for datacenters aimed at both research and education.

![opendc-frontend-construction](misc/artwork/opendc-frontend-construction.png)

Users can construct datacenters (see above) and define portfolios of scenarios (experiments) to see how these datacenters perform under different workloads and schedulers (see below). 

![opendc-frontend-simulation](misc/artwork/opendc-frontend-simulation.png)

The simulator is accessible both as a ready-to-use website hosted by us at [opendc.org](https://opendc.org), and as source code that users can run locally on their own machine, through Docker.

🛠 OpenDC is a project by the [@Large Research Group](https://atlarge-research.com).

🐟 OpenDC comes bundled with [Capelin](https://repository.tudelft.nl/islandora/object/uuid:d6d50861-86a3-4dd3-a13f-42d84db7af66?collection=education), the capacity planning tool for cloud datacenters based on portfolios of what-if scenarios. More information on how to use and extend Capelin coming soon!

## Architecture

OpenDC consists of four components: a Kotlin simulator, a MongoDB database, a Python Flask [API](/opendc-web/opendc-web-api), and a React.js [frontend](/opendc-web/opendc-web-ui).

<p align="center">
    <img src="https://raw.githubusercontent.com/atlarge-research/opendc/master/misc/artwork/opendc-component-diagram.png" alt="OpenDC Component Diagram">
</p>

On the frontend, users can construct a topology by specifying a datacenter's rooms, racks and machines, and create scenarios to see how a workload trace runs on that topology. The frontend communicates with the web server over SocketIO, through a custom REST request/response layer. For example, the frontend might make a `GET` request to `/api/v1/users/{userId}`, but this request is completed via SocketIO, not plain HTTP requests. Note that the API itself can also be accessed by HTTP.

The (Swagger/OpenAPI compliant) API spec specifies what requests the frontend can make to the web server. To view this specification, go to the [Swagger Editor](https://editor.swagger.io/) and paste in our [opendc-api-spec.yml](opendc-api-spec.yml).

The web server receives API requests and processes them in the database. When the frontend requests to run a new scenario, the web server adds it to the `scenarios` collection in the database and sets its `state` as `QUEUED`.

The simulator monitors the database for `QUEUED` scenarios, and simulates them as they are submitted. The results
of the simulations are processed and aggregated in memory. Afterwards, the aggregated summary is written to the database,
which the frontend can then again retrieve via the web server.

## Setup

### Preamble

The official way to run OpenDC is using Docker. Other options include building and running locally, and building and running to deploy on a server.

For all of these options, you have to create a Google API Console project and client ID, which the OpenDC frontend and web server will use to authenticate users and requests. Follow [these steps](https://developers.google.com/identity/sign-in/web/sign-in) to make such a project. In the 'Authorized JavaScript origins' and 'Authorized redirect URI' fields, be sure to add `http://localhost:8080` (frontend), `http://localhost:8081` (api) and `https://localhost:3000` (frontend dev). Download the JSON of the OAuth 2.0 client ID you created from the Credentials tab, and specifically note the `client_id`, which you'll need to build OpenDC.

### Installing Docker
OpenDC uses [Docker](https://www.docker.com/) and [Docker Compose](https://docs.docker.com/compose/) to orchestrate the
deployment of the software stack. Please refer to [Docker Desktop](https://www.docker.com/products/docker-desktop) for
instructions on how install Docker on your machine.


### Running OpenDC

To build and run the full OpenDC stack locally on Linux or Mac, you first need to clone the project:

```bash
git clone https://github.com/atlarge-research/opendc.git

# Enter the directory
cd opendc/
```

In the directory you just entered, you need to set up a set of environment variables.
To do this, create a file called `.env` in the `opendc` folder. In this file, 
replace `your-google-oauth-client-id` with your `client_id` from the OAuth client ID you created.
For a standard setup, you can leave the other settings as-is.

```.env
MONGO_INITDB_ROOT_USERNAME=root
MONGO_INITDB_ROOT_PASSWORD=rootpassword
MONGO_INITDB_DATABASE=admin
OPENDC_DB=opendc
OPENDC_DB_USERNAME=opendc
OPENDC_DB_PASSWORD=opendcpassword
OPENDC_FLASK_SECRET="This is a secret flask key, please change"
OPENDC_OAUTH_CLIENT_ID=your-google-oauth-client-id
OPENDC_API_BASE_URL=http://localhost:8081
```

We provide a list of default traces for you to experiment with. If you want to add others, place them in the `traces` directory and add entries to the database (see also [the database folder](database/mongo-init-opendc-db.sh))

If you plan to publicly deploy, please also tweak the other settings.
In that case, also check the `docker-compose.yml` and `docker-compose.pod.yml` for further instructions.

Now, start the server:

```bash
# Build the Docker image
docker-compose build

# Start the containers
docker-compose up
```

Wait a few seconds and open `http://localhost:8080` in your browser to use OpenDC. We recommend Google Chrome for the best development experience.

## License
OpenDC is distributed under the MIT license. See [LICENSE.txt](/LICENSE.txt).
