from datetime import datetime

from opendc.models.simulation import Simulation
from opendc.models.user import User
from opendc.util import database, exceptions
from opendc.util.database import Database
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

    access_error = simulation.validate_user_access(request.google_id, False)
    if access_error is not None:
        return access_error

    return Response(200, 'Successfully retrieved simulation', simulation.obj)


def PUT(request):
    """Update a simulation's name."""

    try:
        request.check_required_parameters(body={'simulation': {'name': 'name'}}, path={'simulationId': 'string'})
    except exceptions.ParameterError as e:
        return Response(400, str(e))

    simulation = Simulation.from_id(request.params_path['simulationId'])

    validation_error = simulation.validate()
    if validation_error is not None:
        return validation_error

    access_error = simulation.validate_user_access(request.google_id, True)
    if access_error is not None:
        return access_error

    simulation.set_property('name', request.params_body['simulation']['name'])
    simulation.set_property('datetime_last_edited', Database.datetime_to_string(datetime.now()))
    simulation.update()

    return Response(200, 'Successfully updated simulation.', simulation.obj)


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
