#!/usr/bin/env python3
import json
import os
import sys
import traceback
import urllib.request

import flask_socketio
from dotenv import load_dotenv
from flask import Flask, request, jsonify
from flask_compress import Compress
from flask_cors import CORS
from oauth2client import client, crypt

from opendc.models.user import User
from opendc.util import rest, path_parser, database
from opendc.util.exceptions import AuthorizationTokenError, RequestInitializationError
from opendc.util.json import JSONEncoder

load_dotenv()

TEST_MODE = "OPENDC_FLASK_TESTING" in os.environ

# Set up database if not testing
if not TEST_MODE:
    database.DB.initialize_database(
        user=os.environ['OPENDC_DB_USERNAME'],
        password=os.environ['OPENDC_DB_PASSWORD'],
        database=os.environ['OPENDC_DB'],
        host=os.environ.get('OPENDC_DB_HOST', 'localhost'))

# Set up the core app
FLASK_CORE_APP = Flask(__name__)
FLASK_CORE_APP.config['SECRET_KEY'] = os.environ['OPENDC_FLASK_SECRET']
FLASK_CORE_APP.json_encoder = JSONEncoder

# Set up CORS support
CORS(FLASK_CORE_APP)

compress = Compress()
compress.init_app(FLASK_CORE_APP)

SOCKET_IO_CORE = flask_socketio.SocketIO(FLASK_CORE_APP, cors_allowed_origins="*")


@FLASK_CORE_APP.route('/tokensignin', methods=['POST'])
def sign_in():
    """Authenticate a user with Google sign in"""

    try:
        token = request.form['idtoken']
    except KeyError:
        return 'No idtoken provided', 401

    try:
        idinfo = client.verify_id_token(token, os.environ['OPENDC_OAUTH_CLIENT_ID'])

        if idinfo['aud'] != os.environ['OPENDC_OAUTH_CLIENT_ID']:
            raise crypt.AppIdentityError('Unrecognized client.')

        if idinfo['iss'] not in ['accounts.google.com', 'https://accounts.google.com']:
            raise crypt.AppIdentityError('Wrong issuer.')
    except ValueError:
        url = "https://www.googleapis.com/oauth2/v3/tokeninfo?id_token={}".format(token)
        req = urllib.request.Request(url)
        response = urllib.request.urlopen(url=req, timeout=30)
        res = response.read()
        idinfo = json.loads(res)
    except crypt.AppIdentityError as e:
        return 'Did not successfully authenticate'

    user = User.from_google_id(idinfo['sub'])

    data = {'isNewUser': user.obj is None}

    if user.obj is not None:
        data['userId'] = user.get_id()

    return jsonify(**data)


@FLASK_CORE_APP.route('/api/<string:version>/<path:endpoint_path>', methods=['GET', 'POST', 'PUT', 'DELETE'])
def api_call(version, endpoint_path):
    """Call an API endpoint directly over HTTP."""

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

    print(
        f'HTTP:\t{req.method} to `/{req.path}` resulted in {response.status["code"]}: {response.status["description"]}')
    sys.stdout.flush()

    flask_response = jsonify(json.loads(response.to_JSON()))
    flask_response.status_code = response.status['code']
    return flask_response


@SOCKET_IO_CORE.on('request')
def receive_message(message):
    """"Receive a SocketIO request"""
    (req, res) = _process_message(message)

    print(f'Socket: {req.method} to `/{req.path}` resulted in {res.status["code"]}: {res.status["description"]}')
    sys.stdout.flush()

    flask_socketio.emit('response', res.to_JSON(), json=True)


def _process_message(message):
    """Process a request message and return the response."""

    try:
        req = rest.Request(message)
        res = req.process()

        return req, res

    except AuthorizationTokenError:
        res = rest.Response(401, 'Authorization error')
        res.id = message['id']

    except RequestInitializationError as e:
        res = rest.Response(400, str(e))
        res.id = message['id']

        if not 'method' in message:
            message['method'] = 'UNSPECIFIED'
        if not 'path' in message:
            message['path'] = 'UNSPECIFIED'

    except Exception:
        res = rest.Response(500, 'Internal server error')
        if 'id' in message:
            res.id = message['id']
        traceback.print_exc()

    req = rest.Request()
    req.method = message['method']
    req.path = message['path']

    return req, res


if __name__ == '__main__':
    print("Web server started on 8081")
    SOCKET_IO_CORE.run(FLASK_CORE_APP, host='0.0.0.0', port=8081)
