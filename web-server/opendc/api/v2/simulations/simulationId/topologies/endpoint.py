from datetime import datetime

from opendc.models.simulation import Simulation
from opendc.models.topology import Topology
from opendc.util import exceptions
from opendc.util.rest import Response
from opendc.util.database import Database


def POST(request):
    """Add a new Topology to the specified simulation and return it"""

    request.check_required_parameters(path={'simulationId': 'string'}, body={'topology': {'name': 'string'}})

    simulation = Simulation.from_id(request.params_path['simulationId'])

    simulation.check_exists()
    simulation.check_user_access(request.google_id, True)

    topology = Topology({'name': request.params_body['topology']['name']})
    topology.set_property('datetimeCreated', Database.datetime_to_string(datetime.now()))
    topology.set_property('datetimeLastEdited', Database.datetime_to_string(datetime.now()))
    topology.insert()

    simulation.obj['topologyIds'].append(topology.obj['_id'])
    simulation.set_property('datetimeLastEdited', Database.datetime_to_string(datetime.now()))
    simulation.update()

    return Response(200, 'Successfully inserted topology.', topology.obj)
