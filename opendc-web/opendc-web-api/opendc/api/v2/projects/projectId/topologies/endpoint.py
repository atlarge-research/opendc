from datetime import datetime

from opendc.models.project import Project
from opendc.models.topology import Topology
from opendc.util.rest import Response
from opendc.util.database import Database


def POST(request):
    """Add a new Topology to the specified project and return it"""

    request.check_required_parameters(path={'projectId': 'string'}, body={'topology': {'name': 'string'}})

    project = Project.from_id(request.params_path['projectId'])

    project.check_exists()
    project.check_user_access(request.current_user['sub'], True)

    topology = Topology({
        'projectId': project.get_id(),
        'name': request.params_body['topology']['name'],
        'rooms': request.params_body['topology']['rooms'],
    })

    topology.insert()

    project.obj['topologyIds'].append(topology.get_id())
    project.set_property('datetimeLastEdited', Database.datetime_to_string(datetime.now()))
    project.update()

    return Response(200, 'Successfully inserted topology.', topology.obj)
