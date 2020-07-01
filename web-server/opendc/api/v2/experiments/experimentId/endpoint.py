from opendc.models.experiment import Experiment
from opendc.models.simulation import Simulation
from opendc.util.rest import Response


def GET(request):
    """Get this Experiment."""

    request.check_required_parameters(path={'experimentId': 'string'})

    experiment = Experiment.from_id(request.params_path['experimentId'])

    experiment.check_exists()
    experiment.check_user_access(request.google_id, False)

    return Response(200, 'Successfully retrieved Experiment.', experiment.obj)


def PUT(request):
    """Update this Experiments name."""

    request.check_required_parameters(path={'experimentId': 'string'},
                                      body={
                                          'experiment': {
                                              'name': 'string',
                                          }
                                      })

    experiment = Experiment.from_id(request.params_path['experimentId'])

    experiment.check_exists()
    experiment.check_user_access(request.google_id, True)

    experiment.set_property('name', request.params_body['experiment']['name'])

    experiment.update()

    return Response(200, 'Successfully updated experiment.', experiment.obj)


def DELETE(request):
    """Delete this Experiment."""

    request.check_required_parameters(path={'experimentId': 'string'})

    experiment = Experiment.from_id(request.params_path['experimentId'])

    experiment.check_exists()
    experiment.check_user_access(request.google_id, True)

    simulation = Simulation.from_id(experiment.obj['simulationId'])
    simulation.check_exists()
    if request.params_path['experimentId'] in simulation.obj['experimentIds']:
        simulation.obj['experimentIds'].remove(request.params_path['experimentId'])
    simulation.update()

    old_object = experiment.delete()

    return Response(200, 'Successfully deleted experiment.', old_object)
