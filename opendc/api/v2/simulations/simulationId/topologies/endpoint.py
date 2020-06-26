from datetime import datetime

from opendc.models.topology import Topology
from opendc.util import exceptions
from opendc.util.rest import Response
from opendc.models.user import User
from opendc.util.database import Database


def POST(request):
    """Add a new Topology and return it"""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(body={'topology': {'name': 'string'}})
    except exceptions.ParameterError as e:
        return Response(400, str(e))

    # Instantiate a Topology object from our request, and add some metadata 

    topology = Topology({'name': request.params_body['topology']['name']})
    topology.set_property('datetimeCreated', Database.datetime_to_string(datetime.now()))
    topology.set_property('datetimeLastEdited', Database.datetime_to_string(datetime.now()))
    topology.insert()

    # Instantiate the user from the request, and add this topology object to their authorizations

    user = User.from_google_id(request.google_id)
    user.obj['authorizations'].append({'topologyId': topology.obj['_id'], 'authorizationLevel': 'OWN'})
    user.update()

    return Response(200, 'Successfully inserted topology.', topology.obj)
