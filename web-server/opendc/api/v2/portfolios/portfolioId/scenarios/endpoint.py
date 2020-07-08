from opendc.models.portfolio import Portfolio
from opendc.util.rest import Response


def POST(request):
    """Add a new Scenario for this Portfolio."""

    request.check_required_parameters(path={'portfolioId': 'string'},
                                      body={
                                          'scenario': {
                                              'name': 'string',
                                              'trace': {
                                                  'traceId': 'string',
                                                  'loadSamplingFraction': 'float',
                                              },
                                              'topology': {
                                                  'topologyId': 'string',
                                              },
                                              'operational': {
                                                  'failuresEnabled': 'bool',
                                                  'performanceInterferenceEnabled': 'bool',
                                                  'schedulerName': 'string',
                                              },
                                          }
                                      })

    portfolio = Portfolio.from_id(request.params_path['portfolioId'])

    portfolio.check_exists()
    portfolio.check_user_access(request.google_id, True)

    scenario = Portfolio(request.params_body['scenario'])

    scenario.set_property('portfolioId', request.params_path['portfolioId'])
    scenario.set_property('simulationState', 'QUEUED')

    scenario.insert()

    portfolio.obj['portfolioIds'].append(portfolio.get_id())
    portfolio.update()

    return Response(200, 'Successfully added Portfolio.', portfolio.obj)
