from datetime import datetime

from opendc.models.simulation import Simulation
from opendc.models.user import User
from opendc.util import database, exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Simulation."""

    try:
        request.check_required_parameters(path={'simulationId': 'string'})
    except exceptions.ParameterError as e:
        return Response(400, str(e))

    simulation = Simulation.from_id(request.params_path['simulationId'])
    validation_error = simulation.validate()
    if validation_error is not None:
        return validation_error

    user = User.from_google_id(request.google_id)
    authorizations = list(filter(
        lambda x: str(x['simulationId']) == str(request.params_path['simulationId']),
        user.obj['authorizations']))
    if len(authorizations) == 0 or authorizations[0]['authorizationLevel'] == 'VIEW':
        return Response(403, "Forbidden from retrieving simulation.")

    return Response(200, 'Successfully retrieved simulation', simulation.obj)


def PUT(request):
    """Update a simulation's name."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(body={'simulation': {'name': 'name'}}, path={'simulationId': 'string'})

    except exceptions.ParameterError as e:
        return Response(400, str(e))

    # Instantiate a Simulation and make sure it exists

    simulation = Simulation.from_primary_key((request.params_path['simulationId'], ))

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

    return Response(200, 'Successfully updated {}.'.format(simulation), simulation.to_JSON())


def DELETE(request):
    """Delete this Simulation."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(path={'simulationId': 'string'})

    except exceptions.ParameterError as e:
        return Response(400, str(e))

    # Instantiate a Simulation and make sure it exists

    simulation = Simulation.from_primary_key((request.params_path['simulationId'], ))

    if not simulation.exists():
        return Response(404, '{} not found.'.format(simulation))

    # Make sure this User is allowed to delete this Simulation

    if not simulation.google_id_has_at_least(request.google_id, 'OWN'):
        return Response(403, 'Forbidden from deleting {}.'.format(simulation))

    # Delete this Simulation from the database

    simulation.delete()

    # Return this Simulation

    return Response(200, 'Successfully deleted {}.'.format(simulation), simulation.to_JSON())
