from opendc.models.authorization import Authorization
from opendc.models.simulation import Simulation
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Find all authorizations for a Simulation."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'simulationId': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Simulation and make sure it exists

    simulation = Simulation.from_primary_key((request.params_path['simulationId'],))

    if not simulation.exists():
        return Response(404, '{} not found.'.format(simulation))

    # Make sure this User is allowed to view this Simulation's Authorizations

    if not simulation.google_id_has_at_least(request.google_id, 'VIEW'):
        return Response(403, 'Forbidden from retrieving Authorizations for {}.'.format(simulation))

    # Get the Authorizations

    authorizations = Authorization.query('simulation_id', request.params_path['simulationId'])

    # Return the Authorizations

    return Response(
        200,
        'Successfully retrieved Authorizations for {}.'.format(simulation),
        [x.to_JSON() for x in authorizations]
    )
