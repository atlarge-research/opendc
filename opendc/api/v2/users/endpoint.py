from opendc.models.user import User
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Search for a User using their email address."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            query={
                'email': 'string'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate and read a User from the database

    user = User.from_email(request.params_query['email'])

    # Make sure this User exists in the database

    if not user.exists():
        return Response(404, '{} not found'.format(user))

    # Return this User

    return Response(
        200,
        'Successfully retrieved {}.'.format(user),
        user.to_JSON()
    )


def POST(request):
    """Add a new User."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            body={
                'user': {
                    'email': 'string'
                }
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a User

    request.params_body['user']['googleId'] = request.google_id
    user = User.from_JSON(request.params_body['user'])

    # Make sure a User with this Google ID does not already exist

    if user.exists('google_id'):
        user = user.from_google_id(user.google_id)
        return Response(409, '{} already exists.'.format(user))

    # Make sure this User is authorized to create this User

    if not request.google_id == user.google_id:
        return Response(403, 'Forbidden from creating this User.')

    # Insert the User

    user.insert()

    # Return a JSON representation of the User

    return Response(
        200,
        'Successfully created {}'.format(user),
        user.to_JSON()
    )
