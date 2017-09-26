import importlib
import json
import sys

from oauth2client import client, crypt

from opendc.util import exceptions, parameter_checker

with open(sys.argv[1]) as f:
    KEYS = json.load(f)


class Request(object):
    """WebSocket message to REST request mapping."""

    def __init__(self, message=None):
        """"Initialize a Request from a socket message."""

        # Get the Request parameters from the message

        if message is None:
            return

        try:
            self.message = message

            self.id = message['id']

            self.path = message['path']
            self.method = message['method']

            self.params_body = message['parameters']['body']
            self.params_path = message['parameters']['path']
            self.params_query = message['parameters']['query']

            self.token = message['token']

        except KeyError as exception:
            raise exceptions.MissingRequestParameterError(exception)

        # Parse the path and import the appropriate module

        try:
            self.path = message['path'].encode('ascii', 'ignore').strip('/')

            module_base = 'opendc.api.{}.endpoint'
            module_path = self.path.translate(None, '{}').replace('/', '.')

            self.module = importlib.import_module(module_base.format(module_path))

        except UnicodeError:
            raise exceptions.UnimplementedEndpointError('Non-ASCII path')

        except ImportError:
            raise exceptions.UnimplementedEndpointError(
                'Unimplemented endpoint: {}.'.format(self.path)
            )

        # Check the method

        if self.method not in ['POST', 'GET', 'PUT', 'PATCH', 'DELETE']:
            raise exceptions.UnsupportedMethodError('Non-rest method: {}'.format(self.method))

        if not hasattr(self.module, self.method):
            raise exceptions.UnsupportedMethodError(
                'Unimplemented method at endpoint {}: {}'.format(self.path, self.method))

        # Verify the user

        try:
            self.google_id = self._verify_token(self.token)

        except crypt.AppIdentityError as e:
            raise exceptions.AuthorizationTokenError(e.message)

    def _verify_token(self, token):
        """Return the ID of the signed-in user.
        
        Or throw an Exception if the token is invalid.
        """

        try:
            idinfo = client.verify_id_token(token, KEYS['OAUTH_CLIENT_ID'])
        except Exception as e:
            raise crypt.AppIdentityError('Exception caught trying to verify ID token: {}'.format(e))

        if idinfo['aud'] != KEYS['OAUTH_CLIENT_ID']:
            raise crypt.AppIdentityError('Unrecognized client.')

        if idinfo['iss'] not in ['accounts.google.com', 'https://accounts.google.com']:
            raise crypt.AppIdentityError('Wrong issuer.')

        return idinfo['sub']

    def check_required_parameters(self, **kwargs):
        """Raise an error if a parameter is missing or of the wrong type."""

        parameter_checker.check(self, **kwargs)

    def process(self):
        """Process the Request and return a Response."""

        method = getattr(self.module, self.method)

        response = method(self)
        response.id = self.id

        return response

    def to_JSON(self):
        """Return a JSON representation of this Request"""

        self.message['id'] = 0
        self.message['token'] = None

        return json.dumps(self.message)


class Response(object):
    """Response to websocket mapping"""

    def __init__(self, status_code, status_description, content=None):
        """Initialize a new Response."""

        self.status = {
            'code': status_code,
            'description': status_description
        }
        self.content = content

    def to_JSON(self):
        """"Return a JSON representation of this Response"""

        data = {
            'id': self.id,
            'status': self.status
        }

        if self.content is not None:
            data['content'] = self.content

        return json.dumps(data)
