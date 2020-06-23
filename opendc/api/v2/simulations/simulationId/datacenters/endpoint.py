from opendc.models.datacenter import Datacenter
from opendc.models.simulation import Simulation
from opendc.util import exceptions
from opendc.util.rest import Response


def POST(request):
    """Add a new Datacenter to this Simulation."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'simulationId': 'int'
            },
            body={
                'datacenter': {
                    'starred': 'int',
                    'simulationId': 'int'
                }
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Make sure the passed object's simulation id matches the path simulation id

    if request.params_path['simulationId'] != request.params_body['datacenter']['simulationId']:
        return Response(400, 'ID mismatch.')

    # Instantiate a Simulation from the database

    simulation = Simulation.from_primary_key((request.params_path['simulationId'],))

    # Make sure this Simulation exists

    if not simulation.exists():
        return Response(404, '{} not found.'.format(simulation))

    # Make sure this user is authorized to edit this Simulation's Databases

    if not simulation.google_id_has_at_least(request.google_id, 'EDIT'):
        return Response(403, 'Forbidden from adding a datacenter to {}.'.format(simulation))

    # Instantiate a Datacenter

    datacenter = Datacenter.from_JSON(request.params_body['datacenter'])

    datacenter.insert()

    # return this Datacenter

    datacenter.read()

    return Response(
        200,
        'Successfully added {}.'.format(datacenter),
        datacenter.to_JSON()
    )
