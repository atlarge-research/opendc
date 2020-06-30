from opendc.models.experiment import Experiment
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Experiment."""

    request.check_required_parameters(path={'experimentId': 'string'})

    experiment = Experiment.from_id(request.params_path['experimentId'])

    experiment.check_exists()
    experiment.check_user_access(request.google_id, False)

    return Response(200, f'Successfully retrieved Experiment.', experiment.obj)


def PUT(request):
    """Update this Experiment."""

    request.check_required_parameters(path={'experimentId': 'string'},
                                      body={
                                          'experiment': {
                                              'topologyId': 'string',
                                              'traceId': 'string',
                                              'schedulerName': 'string',
                                              'name': 'string',
                                          }
                                      })

    experiment = Experiment.from_id(request.params_path['experimentId'])

    experiment.check_exists()
    experiment.check_user_access(request.google_id, True)

    experiment.set_property('topologyId', request.params_body['experiment']['topologyId'])
    experiment.set_property('traceId', request.params_body['experiment']['traceId'])
    experiment.set_property('schedulerName', request.params_body['experiment']['schedulerName'])
    experiment.set_property('name', request.params_body['experiment']['name'])

    experiment.update()

    return Response(200, 'Successfully updated experiment', experiment.obj)


def DELETE(request):
    """Delete this Experiment."""

    request.check_required_parameters(path={'experimentId': 'string'})

    experiment = Experiment.from_id(request.params_path['experimentId'])

    experiment.check_exists()
    experiment.check_user_access(request.google_id, True)

    experiment.delete()

    return Response(200, 'Successfully deleted experiment.', experiment.obj)
