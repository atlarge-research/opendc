# OpenDC Web Server

## Setup

The following steps will guide you through setting up the OpenDC web server locally for development. To test individual endpoints, edit `static/index.html`. This guide was tested and developed on Windows 10.

Make sure you have Python 2.7 installed (if not, get it [here](https://www.python.org/)), as well as pip (if not, get it [here](https://pip.pypa.io/en/stable/installing/)). Then run the following to install the requirements.

```bash
pip install flask
pip install flask_socketio
pip install oauth2client
pip install eventlet
```

Clone both this repository and the main OpenDC repository, from the same base directory.

```bash
git clone https://github.com/tudelft-atlarge/opendc-web-server.git
git clone https://github.com/tudelft-atlarge/opendc.git
```

Set up the database, replacing `PATH_TO_DATABASE` with where you'd like to create the SQLite database. (This will replace any file named `opendc.db` at the location `PATH_TO_DATABASE`.)

```bash
python opendc/database/rebuild-database.py "PATH_TO_DATABASE"
```

Create a file `config.json` in `opendc-web-server`, containing the following. Replace `BASE_DIRECTORY` with the base directory in which you cloned `opendc` and `opendc-web-server`. Replace `OAUTH_CLIENT_ID` with your OAuth client ID (see the [OpenDC README](https://github.com/tudelft-atlarge/opendc#preamble)). Replace `PATH_TO_DATABASE` with where you created the database.

```json
{
    "ROOT_DIR": "BASE_DIRECTORY",
    "OAUTH_CLIENT_ID": "OAUTH_CLIENT_ID",
    "DATABASE_LOCATION": "PATH_TO_DATABASE\\opendc.db",
    "FLASK_SECRET": "FLASK_SECRET"
}
```

In `opendc-web-server/static/index.html`, add your own `OAUTH_CLIENT_ID` in `content=""`.

Run the server.

```bash
python opendc-web-server/main.py config.json
```