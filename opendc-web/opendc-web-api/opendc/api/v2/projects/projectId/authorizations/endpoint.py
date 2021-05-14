from opendc.models.project import Project
from opendc.util.rest import Response


def GET(request):
    """Find all authorizations for a Project."""

    request.check_required_parameters(path={'projectId': 'string'})

    project = Project.from_id(request.params_path['projectId'])

    project.check_exists()
    project.check_user_access(request.current_user['sub'], False)

    authorizations = project.get_all_authorizations()

    return Response(200, 'Successfully retrieved project authorizations', authorizations)
