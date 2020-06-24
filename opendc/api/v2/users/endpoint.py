from werkzeug.exceptions import abort

from opendc.models.user import User
from opendc.util import exceptions
from opendc.util.database import DB
from opendc.util.rest import Response


def GET(request):
    """Search for a User using their email address."""

    try:
        request.check_required_parameters(query={'email': 'string'})
    except exceptions.ParameterError as e:
        return Response(400, str(e))

    user = DB.fetch_one({'email': request.params_query['email']}, 'users')

    if user is not None:
        return Response(404, f'User with email {request.params_query["email"]} not found')

    return Response(200, 'Successfully retrieved {}.'.format(user), user.to_JSON())


def POST(request):
    """Add a new User."""

    try:
        request.check_required_parameters(body={'user': {'email': 'string'}})
    except exceptions.ParameterError as e:
        return Response(400, str(e))

    request.params_body['user']['googleId'] = request.google_id
    user = request.params_body['user']
    existing_user = DB.fetch_one({'googleId': user['googleId']}, 'users')

    if existing_user is not None:
        return Response(409, '{} already exists.'.format(existing_user))

    if not request.google_id == user['googleId']:
        return Response(403, 'Forbidden from creating this User.')

    user = insert(user, 'users')

    return Response(200, 'Successfully created {}'.format(user), user)
