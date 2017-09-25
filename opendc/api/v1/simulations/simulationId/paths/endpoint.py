from opendc.models.path import Path
from opendc.models.simulation import Simulation
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Simulation's Paths."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'simulationId': 'int'
            }
        )
    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Simulation from the database

    simulation = Simulation.from_primary_key((request.params_path['simulationId'],))

    # Make sure this Simulation exists

    if not simulation.exists():
        return Response(404, '{} not found.'.format(simulation))

    # Make sure this user is authorized to view this Simulation's path

    if not simulation.google_id_has_at_least(request.google_id, 'VIEW'):
        return Response(403, 'Forbidden from viewing Paths for {}.'.format(simulation))

    # Get and return the Paths

    paths = Path.query('simulation_id', request.params_path['simulationId'])

    return Response(
        200,
        'Successfully retrieved Paths for {}.'.format(simulation),
        [x.to_JSON() for x in paths]
    )
