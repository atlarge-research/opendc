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

    print(user['googleId'], request.google_id)
    if user['googleId'] != request.google_id:
        return Response(403, f'Forbidden from editing {user}.')

    user['givenName'] = request.params_body['user']['givenName']
    user['familyName'] = request.params_body['user']['familyName']

    DB.update(user_id, user, 'users')

    return Response(200, f'Successfully updated {user}.', user)


def DELETE(request):
    """Delete this User."""

    try:
        request.check_required_parameters(path={'userId': 'string'})
    except exceptions.ParameterError as e:
        return Response(400, str(e))

    user_id = request.params_path['userId']
    user = DB.fetch_one({'_id': user_id}, 'users')

    if user is None:
        return Response(404, f'User with ID {user_id} not found.')

    if user['googleId'] != request.google_id:
        return Response(403, f'Forbidden from editing {user}.')

    DB.delete_one({'_id': user_id}, 'users')

    return Response(200, f'Successfully deleted {user}.', user)
