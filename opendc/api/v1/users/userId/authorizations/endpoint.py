from opendc.models.authorization import Authorization
from opendc.models.user import User
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this User's Authorizations."""

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

    # Make sure this requester is allowed to retrieve this User's Authorizations

    if not user.google_id_has_at_least(request.google_id, 'OWN'):
        return Response(403, 'Forbidden from retrieving Authorizations for {}.'.format(user))

    # Return this User's Authorizations

    authorizations = Authorization.query('user_id', request.params_path['userId'])

    return Response(
        200,
        'Successfully retrieved Authorizations for {}.'.format(user),
        [x.to_JSON() for x in authorizations]
    )
