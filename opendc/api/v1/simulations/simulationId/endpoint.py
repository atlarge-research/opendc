from datetime import datetime

from opendc.models.simulation import Simulation
from opendc.util import database, exceptions
from opendc.util.rest import Response


def DELETE(request):
    """Delete this Simulation."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'simulationId': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Simulation and make sure it exists

    simulation = Simulation.from_primary_key((request.params_path['simulationId'],))

    if not simulation.exists():
        return Response(404, '{} not found.'.format(simulation))

    # Make sure this User is allowed to delete this Simulation

    if not simulation.google_id_has_at_least(request.google_id, 'OWN'):
        return Response(403, 'Forbidden from deleting {}.'.format(simulation))

    # Delete this Simulation from the database

    simulation.delete()

    # Return this Simulation

    return Response(
        200,
        'Successfully deleted {}.'.format(simulation),
        simulation.to_JSON()
    )


def GET(request):
    """Get this Simulation."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'simulationId': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Simulation and make sure it exists

    simulation = Simulation.from_primary_key((request.params_path['simulationId'],))

    if not simulation.exists():
        return Response(404, '{} not found.'.format(simulation))

    # Make sure this User is allowed to view this Simulation

    if not simulation.google_id_has_at_least(request.google_id, 'VIEW'):
        return Response(403, 'Forbidden from retrieving {}.'.format(simulation))

    # Return this Simulation

    simulation.read()

    return Response(
        200,
        'Successfully retrieved {}'.format(simulation),
        simulation.to_JSON()
    )


def PUT(request):
    """Update a simulation's name."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            body={
                'simulation': {
                    'name': 'name'
                }
            },
            path={
                'simulationId': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Simulation and make sure it exists

    simulation = Simulation.from_primary_key((request.params_path['simulationId'],))

    if not simulation.exists():
        return Response(404, '{} not found.'.format(simulation))

    # Make sure this User is allowed to edit this Simulation

    if not simulation.google_id_has_at_least(request.google_id, 'EDIT'):
        return Response(403, 'Forbidden from editing {}.'.format(simulation))

    # Update this Simulation in the database

    simulation.read()

    simulation.name = request.params_body['simulation']['name']
    simulation.datetime_last_edited = database.datetime_to_string(datetime.now())

    simulation.update()

    # Return this Simulation

    return Response(
        200,
        'Successfully updated {}.'.format(simulation),
        simulation.to_JSON()
    )
