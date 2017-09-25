from opendc.models.experiment import Experiment
from opendc.models.task_state import TaskState
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Experiment's Task States."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'experimentId': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate an Experiment from the database

    experiment = Experiment.from_primary_key((request.params_path['experimentId'],))

    # Make sure this Experiment exists

    if not experiment.exists():
        return Response(404, '{} not found.'.format(experiment))

    # Make sure this user is authorized to view Task States for this Experiment

    if not experiment.google_id_has_at_least(request.google_id, 'VIEW'):
        return Response(403, 'Forbidden from viewing Task States for {}.'.format(experiment))

    # Get and return the Task States

    if 'tick' in request.params_query:
        task_states = TaskState.from_experiment_id_and_tick(
            request.params_path['experimentId'],
            request.params_query['tick']
        )

    else:
        task_states = TaskState.query('experiment_id', request.params_path['experimentId'])

    return Response(
        200,
        'Successfully retrieved Task States for {}.'.format(experiment),
        [x.to_JSON() for x in task_states]
    )
