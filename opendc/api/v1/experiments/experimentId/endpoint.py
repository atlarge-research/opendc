from opendc.models.experiment import Experiment
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Experiment."""

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

    # Make sure this user is authorized to view this Experiment

    if not experiment.google_id_has_at_least(request.google_id, 'VIEW'):
        return Response(403, 'Forbidden from retrieving {}.'.format(experiment))

    # Return this Experiment

    experiment.read()

    return Response(
        200,
        'Successfully retrieved {}.'.format(experiment),
        experiment.to_JSON()
    )


def PUT(request):
    """Update this Experiment's Path, Trace, Scheduler, and/or name."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'experimentId': 'int'
            },
            body={
                'experiment': {
                    'pathId': 'int',
                    'traceId': 'int',
                    'schedulerName': 'string',
                    'name': 'string'
                }
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate an Experiment from the database

    experiment = Experiment.from_primary_key((request.params_path['experimentId'],))

    # Make sure this Experiment exists

    if not experiment.exists():
        return Response(404, '{} not found.'.format(experiment))

    # Make sure this user is authorized to edit this Experiment

    if not experiment.google_id_has_at_least(request.google_id, 'EDIT'):
        return Response(403, 'Forbidden from updating {}.'.format(experiment))

    # Update this Experiment

    experiment.path_id = request.params_body['experiment']['pathId']
    experiment.trace_id = request.params_body['experiment']['traceId']
    experiment.scheduler_name = request.params_body['experiment']['schedulerName']
    experiment.name = request.params_body['experiment']['name']

    try:
        experiment.update()

    except exceptions.ForeignKeyError:
        return Response(400, 'Foreign key error.')

    # Return this Experiment

    return Response(
        200,
        'Successfully updated {}.'.format(experiment),
        experiment.to_JSON()
    )


def DELETE(request):
    """Delete this Experiment."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'experimentId': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate an Experiment and make sure it exists

    experiment = Experiment.from_primary_key((request.params_path['experimentId'],))

    if not experiment.exists():
        return Response(404, '{} not found.'.format(experiment))

    # Make sure this user is authorized to delete this Experiment

    if not experiment.google_id_has_at_least(request.google_id, 'EDIT'):
        return Response(403, 'Forbidden from deleting {}.'.format(experiment))

    # Delete and return this Experiment

    experiment.delete()

    return Response(
        200,
        'Successfully deleted {}.'.format(experiment),
        experiment.to_JSON()
    )
