from datetime import datetime

from opendc.util.database import Database
from opendc.models.simulation import Simulation
from opendc.models.topology import Topology
from opendc.util.rest import Response


def GET(request):
    """Get this Topology."""

    request.check_required_parameters(path={'topologyId': 'int'})

    topology = Topology.from_id(request.params_path['topologyId'])

    topology.check_exists()
    topology.check_user_access(request.google_id, False)

    return Response(200, 'Successfully retrieved topology.', topology.obj)


def PUT(request):
    """Update this topology"""
    request.check_required_parameters(path={'topologyId': 'int'}, body={'topology': {'name': 'string', 'rooms': {}}})
    topology = Topology.from_id(request.params_path['topologyId'])

    topology.check_exists()
    topology.check_user_access(request.google_id, True)

    topology.set_property('name', request.params_body['topology']['name'])
    topology.set_property('rooms', request.params_body['topology']['rooms'])
    topology.set_property('datetimeLastEdited', Database.datetime_to_string(datetime.now()))

    topology.update()

    return Response(200, 'Successfully updated topology.', topology.obj)


def DELETE(request):
    """Delete this topology"""
    request.check_required_parameters(path={'topologyId': 'int'})

    topology = Topology.from_id(request.params_path['topologyId'])

    topology.check_exists()
    topology.check_user_access(request.google_id, True)

    simulation = Simulation.from_id(topology.obj['simulationId'])
    simulation.check_exists()
    if request.params_path['topologyId'] in simulation.obj['topologyIds']:
        simulation.obj['topologyIds'].remove(request.params_path['topologyId'])
    simulation.update()

    topology.delete()

    return Response(200, 'Successfully deleted topology.', topology.obj)
