from opendc.models.portfolio import Portfolio
from opendc.models.project import Project
from opendc.util.rest import Response


def POST(request):
    """Add a new Portfolio for this Project."""

    request.check_required_parameters(path={'projectId': 'string'},
                                      body={
                                          'portfolio': {
                                              'name': 'string',
                                              'targets': {
                                                  'enabledMetrics': 'list',
                                                  'repeatsPerScenario': 'int',
                                              },
                                          }
                                      })

    project = Project.from_id(request.params_path['projectId'])

    project.check_exists()
    project.check_user_access(request.google_id, True)

    portfolio = Portfolio(request.params_body['portfolio'])

    portfolio.set_property('projectId', project.get_id())
    portfolio.set_property('scenarioIds', [])

    portfolio.insert()

    project.obj['portfolioIds'].append(portfolio.get_id())
    project.update()

    return Response(200, 'Successfully added Portfolio.', portfolio.obj)
