from opendc.models.experiment import Experiment
from opendc.models.simulation import Simulation
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Simulation's Experiments."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'simulationId': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Simulation from the database

    simulation = Simulation.from_primary_key((request.params_path['simulationId'],))

    # Make sure this Simulation exists

    if not simulation.exists():
        return Response(404, '{} not found.'.format(simulation))

    # Make sure this user is authorized to view this Simulation's Experiments

    if not simulation.google_id_has_at_least(request.google_id, 'VIEW'):
        return Reponse(403, 'Forbidden from viewing Experiments for {}.'.format(simulation))

    # Get and return the Experiments

    experiments = Experiment.query('simulation_id', request.params_path['simulationId'])

    return Response(
        200,
        'Successfully retrieved Experiments for {}.'.format(simulation),
        [x.to_JSON() for x in experiments]
    )


def POST(request):
    """Add a new Experiment for this Simulation."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'simulationId': 'int'
            },
            body={
                'experiment': {
                    'simulationId': 'int',
                    'pathId': 'int',
                    'traceId': 'int',
                    'schedulerName': 'string',
                    'name': 'string'
                }
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Make sure the passed object's simulation id matches the path simulation id

    if request.params_path['simulationId'] != request.params_body['experiment']['simulationId']:
        return Response(403, 'ID mismatch.')

    # Instantiate a Simulation from the database

    simulation = Simulation.from_primary_key((request.params_path['simulationId'],))

    # Make sure this Simulation exists

    if not simulation.exists():
        return Response(404, '{} not found.'.format(simulation))

    # Make sure this user is authorized to edit this Simulation's Experiments

    if not simulation.google_id_has_at_least(request.google_id, 'EDIT'):
        return Response(403, 'Forbidden from adding an experiment to {}.'.format(simulation))

    # Instantiate an Experiment

    experiment = Experiment.from_JSON(request.params_body['experiment'])
    experiment.state = 'QUEUED'
    experiment.last_simulated_tick = 0

    # Try to insert this Experiment

    try:
        experiment.insert()

    except exceptions.ForeignKeyError as e:
        return Response(400, 'Foreign key constraint not met.' + e.message)

    # Return this Experiment

    experiment.read()

    return Response(
        200,
        'Successfully added {}.'.format(experiment),
        experiment.to_JSON()
    )
