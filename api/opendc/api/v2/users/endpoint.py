from opendc.models.user import User
from opendc.util.rest import Response


def GET(request):
    """Search for a User using their email address."""

    request.check_required_parameters(query={'email': 'string'})

    user = User.from_email(request.params_query['email'])

    user.check_exists()

    return Response(200, 'Successfully retrieved user.', user.obj)


def POST(request):
    """Add a new User."""

    request.check_required_parameters(body={'user': {'email': 'string'}})

    user = User(request.params_body['user'])
    user.set_property('googleId', request.google_id)
    user.set_property('authorizations', [])

    user.check_already_exists()

    user.insert()

    return Response(200, 'Successfully created user.', user.obj)
