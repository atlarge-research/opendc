from datetime import datetime

from opendc.models.simulation import Simulation
from opendc.models.topology import Topology
from opendc.models.user import User
from opendc.util import exceptions
from opendc.util.database import Database
from opendc.util.rest import Response


def POST(request):
    """Create a new simulation, and return that new simulation."""

    request.check_required_parameters(body={'simulation': {'name': 'string'}})

    topology = Topology({'name': 'Default topology'})
    topology.insert()

    simulation = Simulation({'simulation': request.params_body['simulation']})
    simulation.set_property('datetimeCreated', Database.datetime_to_string(datetime.now()))
    simulation.set_property('datetimeLastEdited', Database.datetime_to_string(datetime.now()))
    simulation.set_property('topologyIds', [topology.obj['_id']])
    simulation.set_property('experimentIds', [])
    simulation.insert()

    user = User.from_google_id(request.google_id)
    user.obj['authorizations'].append({'simulationId': simulation.obj['_id'], 'authorizationLevel': 'OWN'})
    user.update()

    return Response(200, 'Successfully created simulation.', simulation.obj)
