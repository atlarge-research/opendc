from datetime import datetime

from opendc.models.project import Project
from opendc.models.topology import Topology
from opendc.util.database import Database
from opendc.util.rest import Response


def GET(request):
    """Get the authorized projects of the user"""
    user_id = request.current_user['sub']
    projects = Project.get_for_user(user_id)
    return Response(200, 'Successfully retrieved projects', projects)


def POST(request):
    """Create a new project, and return that new project."""

    request.check_required_parameters(body={'project': {'name': 'string'}})
    user_id = request.current_user['sub']

    topology = Topology({'name': 'Default topology', 'rooms': []})
    topology.insert()

    project = Project(request.params_body['project'])
    project.set_property('datetimeCreated', Database.datetime_to_string(datetime.now()))
    project.set_property('datetimeLastEdited', Database.datetime_to_string(datetime.now()))
    project.set_property('topologyIds', [topology.get_id()])
    project.set_property('portfolioIds', [])
    project.set_property('authorizations', [{'userId': user_id, 'authorizationLevel': 'OWN'}])
    project.insert()

    topology.set_property('projectId', project.get_id())
    topology.update()

    return Response(200, 'Successfully created project.', project.obj)
