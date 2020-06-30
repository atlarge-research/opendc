from opendc.models.experiment import Experiment
from opendc.models.simulation import Simulation
from opendc.util.rest import Response


def POST(request):
    """Add a new Experiment for this Simulation."""

    request.check_required_parameters(path={'simulationId': 'string'},
                                      body={
                                          'experiment': {
                                              'topologyId': 'string',
                                              'traceId': 'string',
                                              'schedulerName': 'string',
                                              'name': 'string',
                                          }
                                      })

    simulation = Simulation.from_id(request.params_path['simulationId'])

    simulation.check_exists()
    simulation.check_user_access(request.google_id, True)

    experiment = Experiment(request.params_body['experiment'])

    experiment.set_property('simulationId', request.params_path['simulationId'])
    experiment.set_property('state', 'QUEUED')
    experiment.set_property('lastSimulatedTick', 0)

    experiment.insert()

    simulation.obj['experimentIds'].append(experiment.obj['_id'])
    simulation.update()

    return Response(200, 'Successfully added Experiment.', experiment.obj)
