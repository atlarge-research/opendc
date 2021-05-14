import importlib
import json

from opendc.util import exceptions, parameter_checker
from opendc.util.exceptions import ClientError
from opendc.util.auth import current_user


class Request:
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
            self.path = message['path'].strip('/')

            module_base = 'opendc.api.{}.endpoint'
            module_path = self.path.replace('{', '').replace('}', '').replace('/', '.')

            self.module = importlib.import_module(module_base.format(module_path))
        except ImportError as e:
            print(e)
            raise exceptions.UnimplementedEndpointError('Unimplemented endpoint: {}.'.format(self.path))

        # Check the method

        if self.method not in ['POST', 'GET', 'PUT', 'PATCH', 'DELETE']:
            raise exceptions.UnsupportedMethodError('Non-rest method: {}'.format(self.method))

        if not hasattr(self.module, self.method):
            raise exceptions.UnsupportedMethodError('Unimplemented method at endpoint {}: {}'.format(
                self.path, self.method))

        self.current_user = current_user

    def check_required_parameters(self, **kwargs):
        """Raise an error if a parameter is missing or of the wrong type."""

        try:
            parameter_checker.check(self, **kwargs)
        except exceptions.ParameterError as e:
            raise ClientError(Response(400, str(e)))

    def process(self):
        """Process the Request and return a Response."""

        method = getattr(self.module, self.method)

        try:
            response = method(self)
        except ClientError as e:
            e.response.id = self.id
            return e.response

        response.id = self.id

        return response

    def to_JSON(self):
        """Return a JSON representation of this Request"""

        self.message['id'] = 0
        self.message['token'] = None

        return json.dumps(self.message)


class Response:
    """Response to websocket mapping"""
    def __init__(self, status_code, status_description, content=None):
        """Initialize a new Response."""

        self.id = 0
        self.status = {'code': status_code, 'description': status_description}
        self.content = content

    def to_JSON(self):
        """"Return a JSON representation of this Response"""

        data = {'id': self.id, 'status': self.status}

        if self.content is not None:
            data['content'] = self.content

        return json.dumps(data, default=str)
