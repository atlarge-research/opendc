# OpenDC Web Server

The OpenDC web server is the bridge between OpenDC's frontend and database. It is built with Flask/SocketIO in Python and implements the OpenAPI-compliant [OpenDC API specification](https://github.com/atlarge-research/opendc/blob/master/opendc-api-spec.json).

This document explains a high-level view of the web server architecture ([jump](#architecture)), and describes how to set up the web server for local development ([jump](#setup-for-local-development)).

## Architecture

The following diagram shows a high-level view of the architecture of the OpenDC web server. Squared-off colored boxes indicate packages (colors become more saturated as packages are nested); rounded-off boxes indicate individual components; dotted lines indicate control flow; and solid lines indicate data flow.

![OpenDC Web Server Component Diagram](https://raw.githubusercontent.com/atlarge-research/opendc-web-server/master/images/opendc-web-server-component-diagram.png)

The OpenDC API is implemented by the `Main Server Loop`, which is the only component in the base package.

### Util Package

The `Util` package handles several miscellaneous tasks:

* `REST`: Parses SockerIO messages into `Request` objects, and calls the appropriate `API` endpoint to get a `Response` object to return to the `Main Server Loop`.
* `Param Checker`: Recursively checks whether required `Request` parameters are present and correctly typed.
* `Exceptions`: Holds definitions for exceptions used throughough the web server.
* `Database API`: Wraps SQLite functionality used by `Models` to read themselves from/ write themselves into the database.

### API Package

The `API` package contains the logic for the HTTP methods in each API endpoint. Packages are structured to mirror the API: the code for the endpoint `GET simulations/authorizations`, for example, would be located at the `Endpoint` inside the `authorizations` package, inside the `simulations` package (so at `api/simulations/authorizations/endpoint.py`).

An `Endpoint` contains methods for each HTTP method it supports, which takes a request as input (such as `def GET(request):`). Typically, such a method checks whether the parameters were passed correctly (using the `Param Checker`); fetches some model from the database; checks whether the data exists and is accessible by the user who made the request; possibly modifies this data and writes it back to the database; and returns a JSON representation of the model.

The `REST` component dynamically imports the appropriate method from the appropriate `Endpoint`, according to request it receives, and executes it.

### Models Package

The `Models` package contains the logic for mapping Python objects to their database representations. This involves an abstract `model` which has methods to `read`, `insert`, `update` and `delete` objects. Extensions of `model`, such as a `User` or `Simulation`, specify some metadata such as their tabular representation in the database and how they map to a JSON object, which the code in `model` uses in the database interaction methods.

`Endpoint`s import these `Models` and use them to execute requests.

## Setup for Local Development

The following steps will guide you through setting up the OpenDC web server locally for development. To test individual endpoints, edit `static/index.html`. This guide was tested and developed on Windows 10.

### Local Setup

#### Install requirements

Make sure you have Python 2.7 installed (if not, get it [here](https://www.python.org/)), as well as pip (if not, get it [here](https://pip.pypa.io/en/stable/installing/)). Then run the following to install the requirements.

```bash
python setup.py install
```

#### Get the code

Clone both this repository and the main OpenDC repository, from the same base directory.

```bash
git clone https://github.com/atlarge-research/opendc-web-server.git
git clone https://github.com/atlarge-research/opendc.git
```

#### Set up the database

Set up the database, replacing `PATH_TO_DATABASE` with where you'd like to create the SQLite database. (This will replace any file named `opendc.db` at the location `PATH_TO_DATABASE`.)

```bash
cd opendc/database
python rebuild-database.py "PATH_TO_DATABASE"
```

#### Configure OpenDC

Create a file `config.json` in `opendc-web-server`, containing:

```json
{
    "ROOT_DIR": "BASE_DIRECTORY",
    "OAUTH_CLIENT_ID": "OAUTH_CLIENT_ID",
    "DATABASE_LOCATION": "PATH_TO_DATABASE\\opendc.db",
    "FLASK_SECRET": "FLASK_SECRET"
}
```

Make the following replacements:
* Replace `BASE_DIRECTORY` with the base directory in which you cloned `opendc` and `opendc-web-server`.
* Replace `OAUTH_CLIENT_ID` with your OAuth client ID (see the [OpenDC README](https://github.com/atlarge-research/opendc#preamble)).
* Replace `PATH_TO_DATABASE` with where you created the database.
* Replace `FLASK_SECRET`, come up with some string.

In `opendc-web-server/static/index.html`, add your own `OAUTH_CLIENT_ID` in `content=` on line `2`.

#### Set up Postman and OpenDC account

To easily make HTTP requests to the web server, we recommend Postman (get it [here](https://www.getpostman.com/)).

Once Postman is installed and set up, `Import` the OpenDC requests collection (`OpenDC.postman_collection.json`). In the `Collections` tab, expand `OpenDC` and click `Create New User`. This should open the request in the `Builder` pane.

Navigate to `http://localhost:8081/my-auth-token` and copy the authentication token on this page to your clipboard. In the Postman `Builder` pane, navigate to the `Headers (2)` tab, and paste the authentication token as value for the `auth-token` header. (This token expires every hour - refresh the auth token page to get a new token.) 

(Optional: navigate to the `Body` tab and change the email address to the gmail address you used to get an authentication token.)

Click `Send` in Postman to send your request and see the server's response. If it's a `200`, your account is set up!

### Local Development

Run the server.

```bash
cd opendc-web-server
python main.py config.json
```

To try a different query, use the Postman `Builder` to edit the method, path, body, query parameters, etc. `Create New Simulation` is provided as an additional example.

When editing the web server code, restart the server (`CTRL` + `c` followed by `python main.py config.json` in the console running the server) to see the result of your changes.
