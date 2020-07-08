from datetime import datetime

from opendc.models.portfolio import Portfolio
from opendc.models.project import Project
from opendc.models.topology import Topology
from opendc.models.user import User
from opendc.util.database import Database
from opendc.util.rest import Response


def GET(request):
    """Get this Project."""

    request.check_required_parameters(path={'projectId': 'string'})

    project = Project.from_id(request.params_path['projectId'])

    project.check_exists()
    project.check_user_access(request.google_id, False)

    return Response(200, 'Successfully retrieved project', project.obj)


def PUT(request):
    """Update a project's name."""

    request.check_required_parameters(body={'project': {'name': 'name'}}, path={'projectId': 'string'})

    project = Project.from_id(request.params_path['projectId'])

    project.check_exists()
    project.check_user_access(request.google_id, True)

    project.set_property('name', request.params_body['project']['name'])
    project.set_property('datetime_last_edited', Database.datetime_to_string(datetime.now()))
    project.update()

    return Response(200, 'Successfully updated project.', project.obj)


def DELETE(request):
    """Delete this Project."""

    request.check_required_parameters(path={'projectId': 'string'})

    project = Project.from_id(request.params_path['projectId'])

    project.check_exists()
    project.check_user_access(request.google_id, True)

    for topology_id in project.obj['topologyIds']:
        topology = Topology.from_id(topology_id)
        topology.delete()

    for portfolio_id in project.obj['portfolioIds']:
        portfolio = Portfolio.from_id(portfolio_id)
        portfolio.delete()

    user = User.from_google_id(request.google_id)
    user.obj['authorizations'] = list(
        filter(lambda x: str(x['projectId']) != request.params_path['projectId'], user.obj['authorizations']))
    user.update()

    old_object = project.delete()

    return Response(200, 'Successfully deleted project.', old_object)
