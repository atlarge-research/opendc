from datetime import datetime

from opendc.models.simulation import Simulation
from opendc.models.topology import Topology
from opendc.util import exceptions
from opendc.util.rest import Response
from opendc.util.database import Database


def POST(request):
    """Add a new Topology to the specified simulation and return it"""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(path={'simulationId': 'string'}, body={'topology': {'name': 'string'}})
    except exceptions.ParameterError as e:
        return Response(400, str(e))

    simulation = Simulation.from_id(request.params_path['simulationId'])
    validation_error = simulation.validate()
    if validation_error is not None:
        return validation_error

    access_error = simulation.validate_user_access(request.google_id, False)
    if access_error is not None:
        return access_error

    topology = Topology({'name': request.params_body['topology']['name']})
    topology.set_property('datetimeCreated', Database.datetime_to_string(datetime.now()))
    topology.set_property('datetimeLastEdited', Database.datetime_to_string(datetime.now()))
    topology.insert()

    simulation.obj['topologyIds'].append(topology.obj['_id'])
    simulation.set_property('datetimeLastEdited', Database.datetime_to_string(datetime.now()))
    simulation.update()
    # Instantiate the user from the request, and add this topology object to their authorizations

    return Response(200, 'Successfully inserted topology.', topology.obj)
