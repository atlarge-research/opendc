from opendc.models.topology import Topology
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Topology."""

    request.check_required_parameters(path={'topologyId': 'int'})

    topology = Topology.from_id(request.params_path['topologyId'])

    topology.check_exists()
    topology.check_user_access(request.google_id, False)

    return Response(200, 'Successfully retrieved topology.', topology.obj)

def PUT(request):
    pass

def DELETE(request):
    request.check_required_parameters(path={'topologyId': 'int'})

    topology = Topology.from_id(request.params_path['topologyId'])
    
    topology.check_exists()
    topology.delete()

    return Response(200, f'Successfully deleted topology.', topology.obj)