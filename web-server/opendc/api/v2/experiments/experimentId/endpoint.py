from opendc.models.experiment import Experiment
from opendc.models.project import Project
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

    request.check_required_parameters(path={'experimentId': 'string'}, body={'experiment': {
        'name': 'string',
    }})

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

    project = Project.from_id(experiment.obj['projectId'])
    project.check_exists()
    if request.params_path['experimentId'] in project.obj['experimentIds']:
        project.obj['experimentIds'].remove(request.params_path['experimentId'])
    project.update()

    old_object = experiment.delete()

    return Response(200, 'Successfully deleted experiment.', old_object)
