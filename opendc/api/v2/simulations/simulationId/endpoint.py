from datetime import datetime

from opendc.models.simulation import Simulation
from opendc.models.user import User
from opendc.util import database, exceptions
from opendc.util.database import Database
from opendc.util.rest import Response


def GET(request):
    """Get this Simulation."""

    request.check_required_parameters(path={'simulationId': 'string'})

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

    request.check_required_parameters(body={'simulation': {'name': 'name'}}, path={'simulationId': 'string'})

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

    request.check_required_parameters(path={'simulationId': 'string'})

    simulation = Simulation.from_id(request.params_path['simulationId'])

    validation_error = simulation.validate()
    if validation_error is not None:
        return validation_error

    access_error = simulation.validate_user_access(request.google_id, True)
    if access_error is not None:
        return access_error

    # FIXME cascading

    simulation.delete()

    return Response(200, f'Successfully deleted simulation.', simulation.obj)
