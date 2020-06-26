from opendc.models.user import User
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this User."""

    request.check_required_parameters(path={'userId': 'string'})

    user = User.from_id(request.params_path['userId'])

    user.check_exists()

    return Response(200, f'Successfully retrieved user.', user.obj)


def PUT(request):
    """Update this User's given name and/or family name."""

    request.check_required_parameters(body={'user': {
        'givenName': 'string',
        'familyName': 'string'
    }},
                                      path={'userId': 'string'})

    user = User.from_id(request.params_path['userId'])

    user.check_exists()
    user.check_correct_user(request.google_id)

    user.set_property('givenName', request.params_body['user']['givenName'])
    user.set_property('familyName', request.params_body['user']['familyName'])

    user.update()

    return Response(200, f'Successfully updated user.', user.obj)


def DELETE(request):
    """Delete this User."""

    request.check_required_parameters(path={'userId': 'string'})

    user = User.from_id(request.params_path['userId'])

    user.check_exists()
    user.check_correct_user(request.google_id)

    user.delete()

    return Response(200, f'Successfully deleted user.', user.obj)
