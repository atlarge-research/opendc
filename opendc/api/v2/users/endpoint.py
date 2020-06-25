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

    user = User.from_email(request.params_query['email'])

    validation_error = user.validate()
    if validation_error is not None:
        return validation_error

    return Response(200, f'Successfully retrieved user.', user.obj)


def POST(request):
    """Add a new User."""

    try:
        request.check_required_parameters(body={'user': {'email': 'string'}})
    except exceptions.ParameterError as e:
        return Response(400, str(e))

    user = User(request.params_body['user'])
    user.set_property('googleId', request.google_id)
    user.set_property('authorizations', [])

    validation_error = user.validate_insertion()
    if validation_error is not None:
        return validation_error

    user.insert()
    return Response(200, f'Successfully created user.', user.obj)
