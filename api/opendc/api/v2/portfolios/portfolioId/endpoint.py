from opendc.models.portfolio import Portfolio
from opendc.models.project import Project
from opendc.util.rest import Response


def GET(request):
    """Get this Portfolio."""

    request.check_required_parameters(path={'portfolioId': 'string'})

    portfolio = Portfolio.from_id(request.params_path['portfolioId'])

    portfolio.check_exists()
    portfolio.check_user_access(request.google_id, False)

    return Response(200, 'Successfully retrieved portfolio.', portfolio.obj)


def PUT(request):
    """Update this Portfolio."""

    request.check_required_parameters(path={'portfolioId': 'string'}, body={'portfolio': {
        'name': 'string',
        'targets': {
            'enabledMetrics': 'list',
            'repeatsPerScenario': 'int',
        },
    }})

    portfolio = Portfolio.from_id(request.params_path['portfolioId'])

    portfolio.check_exists()
    portfolio.check_user_access(request.google_id, True)

    portfolio.set_property('name',
                           request.params_body['portfolio']['name'])
    portfolio.set_property('targets.enabledMetrics',
                           request.params_body['portfolio']['targets']['enabledMetrics'])
    portfolio.set_property('targets.repeatsPerScenario',
                           request.params_body['portfolio']['targets']['repeatsPerScenario'])

    portfolio.update()

    return Response(200, 'Successfully updated portfolio.', portfolio.obj)


def DELETE(request):
    """Delete this Portfolio."""

    request.check_required_parameters(path={'portfolioId': 'string'})

    portfolio = Portfolio.from_id(request.params_path['portfolioId'])

    portfolio.check_exists()
    portfolio.check_user_access(request.google_id, True)

    portfolio_id = portfolio.get_id()

    project = Project.from_id(portfolio.obj['projectId'])
    project.check_exists()
    if portfolio_id in project.obj['portfolioIds']:
        project.obj['portfolioIds'].remove(portfolio_id)
    project.update()

    old_object = portfolio.delete()

    return Response(200, 'Successfully deleted portfolio.', old_object)
