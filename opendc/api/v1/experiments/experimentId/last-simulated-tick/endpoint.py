from opendc.models.experiment import Experiment
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Experiment's last simulated tick."""

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

    # Make sure this user is authorized to view this Experiment's last simulated tick

    if not experiment.google_id_has_at_least(request.google_id, 'VIEW'):
        return Response(403, 'Forbidden from viewing last simulated tick for {}.'.format(experiment))

    return Response(
        200,
        'Successfully retrieved last simulated tick for {}.'.format(experiment),
        {'lastSimulatedTick': experiment.last_simulated_tick}
    )
