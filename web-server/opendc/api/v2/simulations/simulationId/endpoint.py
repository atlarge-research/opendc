from datetime import datetime

from opendc.models.experiment import Experiment
from opendc.models.simulation import Simulation
from opendc.models.topology import Topology
from opendc.models.user import User
from opendc.util.database import Database
from opendc.util.rest import Response


def GET(request):
    """Get this Simulation."""

    request.check_required_parameters(path={'simulationId': 'string'})

    simulation = Simulation.from_id(request.params_path['simulationId'])

    simulation.check_exists()
    simulation.check_user_access(request.google_id, False)

    return Response(200, 'Successfully retrieved simulation', simulation.obj)


def PUT(request):
    """Update a simulation's name."""

    request.check_required_parameters(body={'simulation': {'name': 'name'}}, path={'simulationId': 'string'})

    simulation = Simulation.from_id(request.params_path['simulationId'])

    simulation.check_exists()
    simulation.check_user_access(request.google_id, True)

    simulation.set_property('name', request.params_body['simulation']['name'])
    simulation.set_property('datetime_last_edited', Database.datetime_to_string(datetime.now()))
    simulation.update()

    return Response(200, 'Successfully updated simulation.', simulation.obj)


def DELETE(request):
    """Delete this Simulation."""

    request.check_required_parameters(path={'simulationId': 'string'})

    simulation = Simulation.from_id(request.params_path['simulationId'])

    simulation.check_exists()
    simulation.check_user_access(request.google_id, True)

    for topology_id in simulation.obj['topologyIds']:
        topology = Topology.from_id(topology_id)
        topology.delete()

    for experiment_id in simulation.obj['experimentIds']:
        experiment = Experiment.from_id(experiment_id)
        experiment.delete()

    user = User.from_google_id(request.google_id)
    user.obj['authorizations'] = list(
        filter(lambda x: str(x['simulationId']) != request.params_path['simulationId'], user.obj['authorizations']))
    user.update()

    old_object = simulation.delete()

    return Response(200, 'Successfully deleted simulation.', old_object)
