from opendc.models.scenario import Scenario
from opendc.models.portfolio import Portfolio
from opendc.util.rest import Response


def GET(request):
    """Get this Scenario."""

    request.check_required_parameters(path={'scenarioId': 'string'})

    scenario = Scenario.from_id(request.params_path['scenarioId'])

    scenario.check_exists()
    scenario.check_user_access(request.google_id, False)

    return Response(200, 'Successfully retrieved scenario.', scenario.obj)


def PUT(request):
    """Update this Scenarios name."""

    request.check_required_parameters(path={'scenarioId': 'string'}, body={'scenario': {
        'name': 'string',
    }})

    scenario = Scenario.from_id(request.params_path['scenarioId'])

    scenario.check_exists()
    scenario.check_user_access(request.google_id, True)

    scenario.set_property('name',
                           request.params_body['scenario']['name'])

    scenario.update()

    return Response(200, 'Successfully updated scenario.', scenario.obj)


def DELETE(request):
    """Delete this Scenario."""

    request.check_required_parameters(path={'scenarioId': 'string'})

    scenario = Scenario.from_id(request.params_path['scenarioId'])

    scenario.check_exists()
    scenario.check_user_access(request.google_id, True)

    portfolio = Portfolio.from_id(scenario.obj['portfolioId'])
    portfolio.check_exists()
    if request.params_path['scenarioId'] in portfolio.obj['scenarioIds']:
        portfolio.obj['scenarioIds'].remove(request.params_path['scenarioId'])
    portfolio.update()

    old_object = scenario.delete()

    return Response(200, 'Successfully deleted scenario.', old_object)
