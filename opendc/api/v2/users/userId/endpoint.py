from opendc.models.user import User
from opendc.util import exceptions
from opendc.util.database import DB
from opendc.util.rest import Response


def GET(request):
    """Get this User."""

    try:
        request.check_required_parameters(path={'userId': 'string'})
    except exceptions.ParameterError as e:
        return Response(400, str(e))

    user = DB.fetch_one({'_id': request.params_path['userId']}, 'users')

    if user is None:
        return Response(404, f'User with ID {request.params_path["userId"]} not found.')

    return Response(200, f'Successfully retrieved {user}.', user)


def PUT(request):
    """Update this User's given name and/or family name."""

    try:
        request.check_required_parameters(body={'user': {
            'givenName': 'string',
            'familyName': 'string'
        }},
                                          path={'userId': 'string'})
    except exceptions.ParameterError as e:
        return Response(400, str(e))

    user_id = request.params_path['userId']
    user = DB.fetch_one({'_id': user_id}, 'users')

    if user is None:
        return Response(404, f'User with ID {user_id} not found.')

    if user['googleId'] != request.google_id:
        return Response(403, f'Forbidden from editing {user}.')

    user['givenName'] = request.params_body['user']['givenName']
    user['familyName'] = request.params_body['user']['familyName']

    DB.update(user_id, user, 'users')

    return Response(200, f'Successfully updated {user}.', user)


def DELETE(request):
    """Delete this user."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(path={'userId': 'string'})

    except exceptions.ParameterError as e:
        return Response(400, str(e))

    # Instantiate a User and make sure they exist

    user = User.from_primary_key((request.params_path['userId'], ))

    if not user.exists():
        return Response(404, '{} not found'.format(user))

    # Make sure this User is allowed to delete this User

    if not user.google_id_has_at_least(request.google_id, 'OWN'):
        return Response(403, 'Forbidden from deleting {}.'.format(user))

    # Delete this User

    user.delete()

    # Return this User

    return Response(200, 'Successfully deleted {}'.format(user), user.to_JSON())
