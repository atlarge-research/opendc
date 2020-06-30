from opendc.models.simulation import Simulation
from opendc.util.rest import Response


def GET(request):
    """Find all authorizations for a Simulation."""

    request.check_required_parameters(path={'simulationId': 'string'})

    simulation = Simulation.from_id(request.params_path['simulationId'])

    simulation.check_exists()
    simulation.check_user_access(request.google_id, False)

    authorizations = simulation.get_all_authorizations()

    return Response(200, 'Successfully retrieved simulation authorizations', authorizations)
