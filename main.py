#!/var/www/opendc.ewi.tudelft.nl/web-server/venv/bin/python

import flask_socketio
import json
import os
import sys
import traceback
import urllib2
from flask import Flask, request, send_from_directory, jsonify
from flask_compress import Compress
from oauth2client import client, crypt
from flask_cors import CORS

from opendc.models.user import User
from opendc.util import exceptions, rest, path_parser, database

if len(sys.argv) < 2:
    print "config file path not given as argument"
    sys.exit(1)

# Get keys from config file
with open(sys.argv[1]) as f:
    KEYS = json.load(f)

STATIC_ROOT = os.path.join(KEYS['ROOT_DIR'], 'opendc-frontend', 'build')

database.init_connection_pool(user=KEYS['MYSQL_USER'], password=KEYS['MYSQL_PASSWORD'],
                              database=KEYS['MYSQL_DATABASE'], host=KEYS['MYSQL_HOST'], port=KEYS['MYSQL_PORT'])

FLASK_CORE_APP = Flask(__name__, static_url_path='', static_folder=STATIC_ROOT)
FLASK_CORE_APP.config['SECREY_KEY'] = KEYS['FLASK_SECRET']
if 'localhost' in KEYS['SERVER_BASE_URL']:
    CORS(FLASK_CORE_APP)

compress = Compress()
compress.init_app(FLASK_CORE_APP)

if 'localhost' in KEYS['SERVER_BASE_URL']:
    SOCKET_IO_CORE = flask_socketio.SocketIO(FLASK_CORE_APP, cors_allowed_origins="*")
else:
    SOCKET_IO_CORE = flask_socketio.SocketIO(FLASK_CORE_APP)

@FLASK_CORE_APP.errorhandler(404)
def page_not_found(e):
    return send_from_directory(STATIC_ROOT, 'index.html')


@FLASK_CORE_APP.route('/tokensignin', methods=['POST'])
def sign_in():
    """Authenticate a user with Google sign in"""

    try:
        token = request.form['idtoken']
    except KeyError:
        return 'No idtoken provided', 401

    try:
        idinfo = client.verify_id_token(token, KEYS['OAUTH_CLIENT_ID'])

        if idinfo['aud'] != KEYS['OAUTH_CLIENT_ID']:
            raise crypt.AppIdentityError('Unrecognized client.')

        if idinfo['iss'] not in ['accounts.google.com', 'https://accounts.google.com']:
            raise crypt.AppIdentityError('Wrong issuer.')
    except ValueError:
        url = "https://www.googleapis.com/oauth2/v3/tokeninfo?id_token={}".format(token)
        req = urllib2.Request(url)
        response = urllib2.urlopen(url=req, timeout=30)
        res = response.read()
        idinfo = json.loads(res)
    except crypt.AppIdentityError, e:
        return 'Did not successfully authenticate'

    user = User.from_google_id(idinfo['sub'])

    data = {
        'isNewUser': not user.exists()
    }

    if user.exists():
        data['userId'] = user.id

    return jsonify(**data)


@FLASK_CORE_APP.route('/api/<string:version>/<path:endpoint_path>', methods=['GET', 'POST', 'PUT', 'DELETE'])
def api_call(version, endpoint_path):
    """Call an API endpoint directly over HTTP"""

    # Get path and parameters
    (path, path_parameters) = path_parser.parse(version, endpoint_path)

    query_parameters = request.args.to_dict()
    for param in query_parameters:
        try:
            query_parameters[param] = int(query_parameters[param])
        except:
            pass

    try:
        body_parameters = json.loads(request.get_data())
    except:
        body_parameters = {}

    # Create and call request
    (req, response) = _process_message({
        'id': 0,
        'method': request.method,
        'parameters': {
            'body': body_parameters,
            'path': path_parameters,
            'query': query_parameters
        },
        'path': path,
        'token': request.headers.get('auth-token')
    })

    print 'HTTP:\t{} to `/{}` resulted in {}: {}'.format(
        req.method,
        req.path,
        response.status['code'],
        response.status['description']
    )
    sys.stdout.flush()

    flask_response = jsonify(json.loads(response.to_JSON()))
    flask_response.status_code = response.status['code']
    return flask_response


@FLASK_CORE_APP.route('/my-auth-token')
def serve_web_server_test():
    """Serve the web server test."""

    return send_from_directory(os.path.join(KEYS['ROOT_DIR'], 'opendc-web-server', 'static'), 'index.html')


@FLASK_CORE_APP.route('/')
@FLASK_CORE_APP.route('/simulations')
@FLASK_CORE_APP.route('/simulations/<path:simulation_id>')
@FLASK_CORE_APP.route('/simulations/<path:simulation_id>/experiments')
@FLASK_CORE_APP.route('/simulations/<path:simulation_id>/experiments/<path:experiment_id>')
@FLASK_CORE_APP.route('/profile')
def serve_index(simulation_id=None, experiment_id=None):
    return send_from_directory(STATIC_ROOT, 'index.html')


@SOCKET_IO_CORE.on('request')
def receive_message(message):
    """"Receive a SocketIO request"""

    (request, response) = _process_message(message)

    print 'Socket:\t{} to `/{}` resulted in {}: {}'.format(
        request.method,
        request.path,
        response.status['code'],
        response.status['description']
    )
    sys.stdout.flush()

    flask_socketio.emit('response', response.to_JSON(), json=True)


def _process_message(message):
    """Process a request message and return the response."""

    try:
        request = rest.Request(message)
        response = request.process()

        return (request, response)

    except exceptions.AuthorizationTokenError as e:
        response = rest.Response(401, 'Authorization error')
        response.id = message['id']

    except exceptions.RequestInitializationError as e:
        response = rest.Response(400, e.message)
        response.id = message['id']

        if not 'method' in message:
            message['method'] = 'UNSPECIFIED'
        if not 'path' in message:
            message['path'] = 'UNSPECIFIED'

    except Exception as e:
        response = rest.Response(500, 'Internal server error')
        if 'id' in message:
            response.id = message['id']
        traceback.print_exc()

    request = rest.Request()
    request.method = message['method']
    request.path = message['path']

    return (request, response)


SOCKET_IO_CORE.run(FLASK_CORE_APP, host='0.0.0.0', port=8081)
