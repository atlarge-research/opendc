<h1 align="center">
    <img src="images/logo.png" width="100" alt="OpenDC">
    <br>
    OpenDC
</h1>
<p align="center">
    Collaborative Datacenter Simulation and Exploration for Everybody
</p>

<br>

OpenDC is an open-source simulator for datacenters aimed at both research and education.

![opendc-frontend-construction](https://raw.githubusercontent.com/tudelft-atlarge/opendc/master/images/opendc-frontend-construction.PNG)

Users can construct datacenters (see above) and define experiments to see how these datacenters perform under different workloads and schedulers (see below). 

![opendc-frontend-simulation](https://raw.githubusercontent.com/tudelft-atlarge/opendc/master/images/opendc-frontend-simulation.PNG)

The simulator is accessible both as a ready-to-use website hosted by Delft University of Technology at [opendc.org](http://opendc.org), and as source code that users can run locally on their own machine.

OpenDC is a project by the [@Large Research Group](http://atlarge-research.com).

## Architecture

OpenDC consists of four components: a Kotlin simulator, a MariaDB database, a Python Flask web server, and a React.js frontend.

<p align="center">
    <img src="https://raw.githubusercontent.com/tudelft-atlarge/opendc/master/images/opendc-component-diagram.png" alt="OpenDC Component Diagram">
</p>

On the frontend, users can construct a topology by specifying a datacenter's rooms, racks and machines, and create experiments to see how a workload trace runs on that topology. The frontend communicates with the web server over SocketIO, through a custom REST request/response layer. For example, the frontend might make a `GET` request to `/api/v1/users/{userId}`, but this request is completed via SocketIO, not plain HTTP requests.

The (Swagger/ OpenAPI compliant) API spec specifies what requests the frontend can make to the web server. To view this specification, go to the [Swagger UI](http://petstore.swagger.io/) and "Explore" [opendc-api-spec.json](https://raw.githubusercontent.com/tudelft-atlarge/opendc/master/opendc-api-spec.json).

The web server receives API requests and processes them in the SQLite database. When the frontend requests to run a new experiment, the web server adds it to the `experiments` table in the database and sets is `state` as `QUEUED`.

The simulator monitors the database for `QUEUED` experiments, and simulates them as they are submitted. It writes the resulting `machine_states` and `task_states` to the database, which the frontend can then again retrieve via the web server.

## Setup

### Preamble

The official way to run OpenDC is using Docker. Other options include building and running locally, and building and running to deploy on a server.

For all of these options, you have to create a Google API Console project and client ID, which the OpenDC frontend and web server will use to authenticate users and requests. Follow [these steps](https://developers.google.com/identity/sign-in/web/devconsole-project) to make such a project. In the 'Authorized JavaScript origins' field, be sure to add `http://localhost:8081` as origin. Download the JSON of the OAuth 2.0 client ID you created from the Credentials tab, and specifically note the `client_id`, which you'll need to build OpenDC.

### Installing Docker

GNU/Linux, Mac OS X and Windows 10 Professional users can install Docker by following the instructions [here](https://www.docker.com/products/docker). 

Users of Windows 10 Home and previous editions of Windows can use [Docker Toolbox](https://www.docker.com/products/docker-toolbox). If you're using the toolbox, don't forget to setup port forwarding (see the following subsection if you haven't done that, yet).

#### Port Forwarding

Open VirtualBox, navigate to the settings of your default docker VM, and go to the 'Network' tab. There, hidden in the 'Advanced' panel, is the 'Port forwarding' feature, where you can set a rule for exposing a port of the VM to the host OS. Add one from guest IP `10.0.2.15` to host IP `127.0.0.1`, both on port `8081`. This enables you to open a browser on your host OS and navigate to `http://localhost:8081`, once the server is running.

### Running OpenDC

To build and run the full OpenDC stack locally on Linux or Mac, you first need to clone the project:

```bash
# Clone the repo and its submodules
git clone --recursive https://github.com/atlarge-research/opendc.git

# Enter the directory
cd opendc/

# If you're on Windows:
# Turn off automatic line-ending conversion in the simulator sub-repository
cd opendc-simulator/
git config core.autocrlf false
cd ..
```

In the directory you just entered, you need to set up a small configuration file. To do this, create a file called `keys.json` in the `opendc` folder. In this file, simply replace `your-google-oauth-client-id` with your `client_id` from the OAuth client ID you created. For a standard setup, you can leave the other settings as-is.

```json
{
  "FLASK_SECRET": "This is a super duper secret flask key",
  "OAUTH_CLIENT_ID": "your-google-oauth-client-id",
  "ROOT_DIR": "/opendc",
  "SERVER_BASE_URL": "http://localhost:8081"
}
```

Now, start the server:

```bash
# Build the Docker image
docker-compose build

# Start the OpenDC container and the database container
docker-compose up
```

Wait a few seconds and open `http://localhost:8081` in your browser to use OpenDC.
