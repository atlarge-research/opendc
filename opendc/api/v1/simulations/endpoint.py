from datetime import datetime

from opendc.models.authorization import Authorization
from opendc.models.datacenter import Datacenter
from opendc.models.path import Path
from opendc.models.section import Section
from opendc.models.simulation import Simulation
from opendc.models.user import User
from opendc.util import database, exceptions
from opendc.util.rest import Response


def POST(request):
    """Create a new simulation, and return that new simulation."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            body={
                'simulation': {
                    'name': 'string'
                }
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Simulation

    simulation_data = request.params_body['simulation']

    simulation_data['datetimeCreated'] = database.datetime_to_string(datetime.now())
    simulation_data['datetimeLastEdited'] = database.datetime_to_string(datetime.now())

    simulation = Simulation.from_JSON(simulation_data)

    # Insert this Simulation into the database

    simulation.insert()

    # Instantiate an Authorization and insert it into the database

    authorization = Authorization(
        user_id=User.from_google_id(request.google_id).id,
        simulation_id=simulation.id,
        authorization_level='OWN'
    )

    authorization.insert()

    # Instantiate a Path and insert it into the database

    path = Path(
        simulation_id=simulation.id,
        datetime_created=database.datetime_to_string(datetime.now())
    )

    path.insert()

    # Instantiate a Datacenter and insert it into the database

    datacenter = Datacenter(
        starred=0,
        simulation_id=simulation.id
    )

    datacenter.insert()

    # Instantiate a Section and insert it into the database

    section = Section(
        path_id=path.id,
        datacenter_id=datacenter.id,
        start_tick=0
    )

    section.insert()

    # Return this Simulation

    return Response(
        200,
        'Successfully created {}.'.format(simulation),
        simulation.to_JSON()
    )
