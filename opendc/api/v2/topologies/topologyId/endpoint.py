from opendc.models.topology import Topology
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Topology."""

    # Make sure required parameters are there

    request.check_required_parameters(path={'topologyId': 'int'})

    # Instantiate a Topology from the database

    topology = Topology.from_id(request.params_path['topologyId'])

    # Make sure this Topology exists

    validation_error = topology.validate()
    if validation_error is not None:
        return validation_error    

    # Make sure this user is authorized to view this Topology

    access_error = topology.validate_user_access(request.google_id, False)
    if access_error is not None:
        return access_error

    # Return this Topology

    return Response(200, 'Successfully retrieved topology.', topology.obj)
