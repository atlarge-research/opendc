# OpenDC

## Architecture

OpenDC consists of four components: a C++ simulator, a SQLite database, a Python Flask web server, and a TypeScript frontend.

On the frontend, users can construct a topology by specifying a datacenter's rooms, racks and machines, and create experiments to see how a workload trace runs on that topology. The frontend communicates with the web server over SocketIO, through a custom REST request/response layer. For example, the frontend might make a `GET` request to `/api/v1/users/{userId}`, but this request is completed via SocketIO, not plain HTTP requests.

The (Swagger/ OpenAPI compliant) API specification specifies what requests the frontend can make to the web server. To view this specification, go to the [Swagger UI](http://petstore.swagger.io/) and "Explore" [opendc-api-spec.json](https://raw.githubusercontent.com/tudelft-atlarge/opendc/master/opendc-api-spec.json).

The web server receives these API requests and processes them in the SQLite database. When the frontend requests to run a new experiment, the web server adds a row to the `queued_experiments` table in the database.

The simulator monitors this `queued_experiments` table, and simulates experiments as they are submitted. It writes the resulting `machine_states` and `task_states` to the database, which the frontend can then again retrieve via the web server.

## Setup

### Preamble

The official way to run OpenDC is using Docker. Other options include building and running locally, and building and running to deploy on a server.

For all of these options, you have to create a Google API Console project and client ID, which the OpenDC frontend and web server will use to authenticate users and requests. Follow [these steps](https://developers.google.com/identity/sign-in/web/devconsole-project) to make such a project. Download the JSON of the OAuth 2.0 client ID you created from the Credentials tab, and specifically note the `client_id` and the `client_secret`, which you'll need to build OpenDC.

### Running OpenDC locally

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

# Wait a few seconds and open http://localhost:8081 in your browser
```
