from opendc.models.user import User
from opendc.util import exceptions
from opendc.util.rest import Response


def DELETE(request):
    """Delete this user."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'userId': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a User and make sure they exist

    user = User.from_primary_key((request.params_path['userId'],))

    if not user.exists():
        return Response(404, '{} not found'.format(user))

    # Make sure this User is allowed to delete this User

    if not user.google_id_has_at_least(request.google_id, 'OWN'):
        return Response(403, 'Forbidden from deleting {}.'.format(user))

    # Delete this User

    user.delete()

    # Return this User

    return Response(
        200,
        'Successfully deleted {}'.format(user),
        user.to_JSON()
    )


def GET(request):
    """Get this User."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'userId': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a User and make sure they exist

    user = User.from_primary_key((request.params_path['userId'],))

    if not user.exists():
        return Response(404, '{} not found.'.format(user))

    # Return this User

    return Response(
        200,
        'Successfully retrieved {}'.format(user),
        user.to_JSON(),
    )


def PUT(request):
    """Update this User's given name and/ or family name."""

    # Make sure the required parameters are there

    try:
        request.check_required_parameters(
            body={
                'user': {
                    'givenName': 'string',
                    'familyName': 'string'
                }
            },
            path={
                'userId': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a User and make sure they exist

    user = User.from_primary_key((request.params_path['userId'],))

    if not user.exists():
        return Response(404, '{} not found.'.format(user))

    # Make sure this User is allowed to edit this User

    if not user.google_id_has_at_least(request.google_id, 'OWN'):
        return Response(403, 'Forbidden from editing {}.'.format(user))

    # Update this User

    user.given_name = request.params_body['user']['givenName']
    user.family_name = request.params_body['user']['familyName']

    user.update()

    # Return this User

    return Response(
        200,
        'Successfully updated {}.'.format(user),
        user.to_JSON()
    )
