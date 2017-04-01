#!/var/www/opendc.ewi.tudelft.nl/web-server/venv/bin/python

import json
import os
import sys
import traceback

from flask import Flask, abort, request, send_from_directory, session, jsonify
import flask_socketio
from oauth2client import client, crypt

from opendc.models.user import User
from opendc.util import exceptions, rest, path_parser

if len(sys.argv) < 2:
    print "config file path not given as argument"
    sys.exit(1)

# Get keys from config file
with open(sys.argv[1]) as file:
    KEYS = json.load(file)

STATIC_ROOT = os.path.join(KEYS['ROOT_DIR'], 'opendc-frontend', 'build')

FLASK_CORE_APP = Flask(__name__, static_url_path='')
FLASK_CORE_APP.config['SECREY_KEY'] = KEYS['FLASK_SECRET']

SOCKET_IO_CORE = flask_socketio.SocketIO(FLASK_CORE_APP)

@FLASK_CORE_APP.errorhandler(404)
def page_not_found(e):
    return send_from_directory(STATIC_ROOT, '404.html')

@FLASK_CORE_APP.route('/')
def serve_splash():
    """Serve the splash page on /"""

    return send_from_directory(STATIC_ROOT, 'index.html')

@FLASK_CORE_APP.route('/app')
def serve_app():
    """Serve the app on /app."""
    
    return send_from_directory(STATIC_ROOT, 'app.html')

@FLASK_CORE_APP.route('/profile')
def serve_profile():
    """Serve profile page."""

    return send_from_directory(STATIC_ROOT, 'profile.html')

@FLASK_CORE_APP.route('/projects')
def serve_projects():
    """Serve the projects page."""

    return send_from_directory(STATIC_ROOT, 'projects.html')

@FLASK_CORE_APP.route('/web-server-test')
def serve_web_server_test():
    """Serve the web server test."""

    return send_from_directory(os.path.join(KEYS['ROOT_DIR'], 'opendc-web-server', 'static'), 'index.html')

@FLASK_CORE_APP.route('/<path:folder>/<path:filepath>')
def serve_static(folder, filepath):
    """Serve static files from the build directory"""

    if not folder in ['bower_components', 'img', 'scripts', 'styles']:
        abort(404)

    return send_from_directory(os.path.join(STATIC_ROOT, folder), filepath)

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

    except crypt.AppIdentityError, e:
        return 'Did not successfully authenticate'

    user = User.from_google_id(idinfo['sub'])

    data = {
        'isNewUser': not user.exists()
    }

    if user.exists():
        data['userId'] = user.id

    return jsonify(**data)

@FLASK_CORE_APP.route('/api/<string:version>/<path:endpoint_path>')
def api_call(version, endpoint_path):
    """Call an API endpoint directly over HTTP"""

    path = path_parser.parse(version, endpoint_path)
    
    return jsonify(path)

@SOCKET_IO_CORE.on('request')
def receive_message(message):
    """"Receive a SocketIO request"""
    
    try:
        request = rest.Request(message)
        response = request.process()
        
        flask_socketio.emit('response', response.to_JSON(), json=True)

        print 'Socket: {} to `/{}` resulted in {}: {}'.format(
            request.method,
            request.path,
            response.status['code'],
            response.status['description']
        )

        return

    except exceptions.AuthorizationTokenError as e:
        response = rest.Response(401, 'Authorization error')
        response.id = message['id']

        flask_socketio.emit('response', response.to_JSON(), json=True)

    except exceptions.RequestInitializationError as e:
        response = rest.Response(400, e.message)
        response.id = message['id']

        flask_socketio.emit('response', response.to_JSON(), json=True)

    except Exception as e:
        response = rest.Response(500, 'Internal server error')
        response.id = message['id']

        flask_socketio.emit('response', response.to_JSON(), json=True)
        traceback.print_exc()

    print 'Socket: {} to `{}` resulted in {}: {}'.format(
        message['method'],
        message['path'],
        response.status['code'],
        response.status['description']
    )


SOCKET_IO_CORE.run(FLASK_CORE_APP, host='0.0.0.0', port=8081)
