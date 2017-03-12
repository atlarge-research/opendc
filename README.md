# OpenDC

## Architecture

OpenDC consists of four components: a C++ simulator, a SQLite database, a Python Flask web server, and a TypeScript frontend.

![opendc-component-diagram](https://raw.githubusercontent.com/tudelft-atlarge/opendc/master/images/opendc-component-diagram.png)

On the frontend, users can construct a topology by specifying a datacenter's rooms, racks and machines, and create experiments to see how a workload trace runs on that topology. The frontend communicates with the web server over SocketIO, through a custom REST request/response layer. For example, the frontend might make a `GET` request to `/api/v1/users/{userId}`, but this request is completed via SocketIO, not plain HTTP requests.

The (Swagger/ OpenAPI compliant) API spec specifies what requests the frontend can make to the web server. To view this specification, go to the [Swagger UI](http://petstore.swagger.io/) and "Explore" [opendc-api-spec.json](https://raw.githubusercontent.com/tudelft-atlarge/opendc/master/opendc-api-spec.json).

The web server receives API requests and processes them in the SQLite database. When the frontend requests to run a new experiment, the web server adds it to the `experiments` table in the database and sets is `state` as `QUEUED`.

The simulator monitors the database for `QUEUED` experiments, and simulates them as they are submitted. It writes the resulting `machine_states` and `task_states` to the database, which the frontend can then again retrieve via the web server.

## Setup

### Preamble

The official way to run OpenDC is using Docker. Other options include building and running locally, and building and running to deploy on a server.

For all of these options, you have to create a Google API Console project and client ID, which the OpenDC frontend and web server will use to authenticate users and requests. Follow [these steps](https://developers.google.com/identity/sign-in/web/devconsole-project) to make such a project. In the 'Authorized JavaScript origins' field, be sure to add `http://localhost:8081` as origin. Download the JSON of the OAuth 2.0 client ID you created from the Credentials tab, and specifically note the `client_id` and the `client_secret`, which you'll need to build OpenDC.

### Installing Docker

GNU/Linux, Mac OS X and Windows 10 Professional users can install Docker by following the instructions [here](https://www.docker.com/products/docker). 

Users of Windows 10 Home and previous editions of Windows can use [Docker Toolbox](https://www.docker.com/products/docker-toolbox). If you're using the toolbox, don't forget to setup port forwarding (see the following subsection if you haven't done that, yet).

#### Port Forwarding

Open VirtualBox, navigate to the settings of your default docker VM, and go to the 'Network' tab. There, hidden in the 'Advanced' panel, is the 'Port forwarding' feature, where you can set a rule for exposing a port of the VM to the host OS. Add one from guest IP `10.0.2.15` to host IP `127.0.0.1`, both on port `8081`. This enables you to open a browser on your host OS and navigate to `http://localhost:8081`, once the server is running.

### Running OpenDC

To build and run the full OpenDC stack locally on Linux or Mac, run the commands bellow. Replace `your-google-oauth-client-id` with your `client_id` from the OAuth 2.0 client ID you created, and replace `your-google-oauth-secret` with your `client_secret`.

```bash
# Clone the repo and its submodules
git clone --recursive https://github.com/tudelft-atlarge/opendc.git

# Enter the directory
cd opendc/

# Build the Docker image
docker build -t="opendc" .

# Start a container with the image
docker run -d --name opendc -p 8081:8081 -e 'SERVER_URL=http://localhost:8081' -e 'OAUTH_CLIENT_ID=your-google-oauth-client-id' -e 'OAUTH_CLIENT_SECRET=your-google-oauth-secret' opendc
```

Wait a few seconds and open `http://localhost:8081` in your browser to use OpenDC.
