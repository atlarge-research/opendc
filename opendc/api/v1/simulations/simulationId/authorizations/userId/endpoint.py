from opendc.models.authorization import Authorization
from opendc.models.simulation import Simulation
from opendc.models.user import User
from opendc.util import exceptions
from opendc.util.rest import Response


def DELETE(request):
    """Delete a user's authorization level over a simulation."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'simulationId': 'int',
                'userId': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate an Authorization

    authorization = Authorization.from_primary_key((
        request.params_path['userId'],
        request.params_path['simulationId']
    ))

    # Make sure this Authorization exists in the database

    if not authorization.exists():
        return Response(404, '{} not found.'.format(authorization))

    # Make sure this User is allowed to delete this Authorization

    if not authorization.google_id_has_at_least(request.google_id, 'OWN'):
        return Response(403, 'Forbidden from deleting {}.'.format(authorization))

    # Delete this Authorization

    authorization.delete()

    return Response(
        200,
        'Successfully deleted {}.'.format(authorization),
        authorization.to_JSON()
    )


def GET(request):
    """Get this User's Authorization over this Simulation."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'simulationId': 'int',
                'userId': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate an Authorization

    authorization = Authorization.from_primary_key((
        request.params_path['userId'],
        request.params_path['simulationId']
    ))

    # Make sure this Authorization exists in the database

    if not authorization.exists():
        return Response(404, '{} not found.'.format(authorization))

    # Read this Authorization from the database

    authorization.read()

    # Return this Authorization

    return Response(
        200,
        'Successfully retrieved {}'.format(authorization),
        authorization.to_JSON()
    )


def POST(request):
    """Add an authorization for a user's access to a simulation."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'userId': 'int',
                'simulationId': 'int'
            },
            body={
                'authorization': {
                    'authorizationLevel': 'string'
                }
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate an Authorization

    authorization = Authorization.from_JSON({
        'userId': request.params_path['userId'],
        'simulationId': request.params_path['simulationId'],
        'authorizationLevel': request.params_body['authorization']['authorizationLevel']
    })

    # Make sure the Simulation and User exist

    user = User.from_primary_key((authorization.user_id,))
    if not user.exists():
        return Response(404, '{} not found.'.format(user))

    simulation = Simulation.from_primary_key((authorization.simulation_id,))
    if not simulation.exists():
        return Response(404, '{} not found.'.format(simulation))

    # Make sure this User is allowed to add this Authorization

    if not simulation.google_id_has_at_least(request.google_id, 'OWN'):
        return Response(403, 'Forbidden from creating {}.'.format(authorization))

    # Make sure this Authorization does not already exist

    if authorization.exists():
        return Response(409, '{} already exists.'.format(authorization))

    # Try to insert this Authorization into the database

    try:
        authorization.insert()

    except exceptions.ForeignKeyError:
        return Response(400, 'Invalid authorizationLevel')

    # Return this Authorization

    return Response(
        200,
        'Successfully added {}'.format(authorization),
        authorization.to_JSON()
    )


def PUT(request):
    """Change a user's authorization level over a simulation."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'simulationId': 'int',
                'userId': 'int'
            },
            body={
                'authorization': {
                    'authorizationLevel': 'string'
                }
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate and Authorization

    authorization = Authorization.from_JSON({
        'userId': request.params_path['userId'],
        'simulationId': request.params_path['simulationId'],
        'authorizationLevel': request.params_body['authorization']['authorizationLevel']
    })

    # Make sure this Authorization exists

    if not authorization.exists():
        return Response(404, '{} not found.'.format(authorization))

    # Make sure this User is allowed to edit this Authorization

    if not authorization.google_id_has_at_least(request.google_id, 'OWN'):
        return Response(403, 'Forbidden from updating {}.'.format(authorization))

    # Try to update this Authorization

    try:
        authorization.update()

    except exceptions.ForeignKeyError as e:
        return Response(400, 'Invalid authorization level.')

    # Return this Authorization

    return Response(
        200,
        'Successfully updated {}.'.format(authorization),
        authorization.to_JSON()
    )
