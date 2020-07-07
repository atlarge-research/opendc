from opendc.models.experiment import Experiment
from opendc.models.project import Project
from opendc.util.rest import Response


def POST(request):
    """Add a new Experiment for this Project."""

    request.check_required_parameters(path={'projectId': 'string'},
                                      body={
                                          'experiment': {
                                              'topologyId': 'string',
                                              'traceId': 'string',
                                              'schedulerName': 'string',
                                              'name': 'string',
                                          }
                                      })

    project = Project.from_id(request.params_path['projectId'])

    project.check_exists()
    project.check_user_access(request.google_id, True)

    experiment = Experiment(request.params_body['experiment'])

    experiment.set_property('projectId', request.params_path['projectId'])
    experiment.set_property('state', 'QUEUED')
    experiment.set_property('lastSimulatedTick', 0)

    experiment.insert()

    project.obj['experimentIds'].append(experiment.get_id())
    project.update()

    return Response(200, 'Successfully added Experiment.', experiment.obj)
