from datetime import datetime

from opendc.models.project import Project
from opendc.models.topology import Topology
from opendc.models.user import User
from opendc.util.database import Database
from opendc.util.rest import Response


def POST(request):
    """Create a new project, and return that new project."""

    request.check_required_parameters(body={'project': {'name': 'string'}})

    topology = Topology({'name': 'Default topology', 'rooms': []})
    topology.insert()

    project = Project(request.params_body['project'])
    project.set_property('datetimeCreated', Database.datetime_to_string(datetime.now()))
    project.set_property('datetimeLastEdited', Database.datetime_to_string(datetime.now()))
    project.set_property('topologyIds', [topology.get_id()])
    project.set_property('portfolioIds', [])
    project.insert()

    topology.set_property('projectId', project.get_id())
    topology.update()

    user = User.from_google_id(request.google_id)
    user.obj['authorizations'].append({'projectId': project.get_id(), 'authorizationLevel': 'OWN'})
    user.update()

    return Response(200, 'Successfully created project.', project.obj)
