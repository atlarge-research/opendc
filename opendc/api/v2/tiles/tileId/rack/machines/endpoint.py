from opendc.models.machine import Machine
from opendc.models.rack import Rack
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Rack's Machines."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'tileId': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Rack from the database

    rack = Rack.from_tile_id(request.params_path['tileId'])

    # Make sure this Rack exists

    if not rack.exists():
        return Response(404, '{} not found.'.format(rack))

    # Make sure this user is authorized to view this Rack's Machines

    if not rack.google_id_has_at_least(request.google_id, 'VIEW'):
        return Response(403, 'Forbidden from viewing {}.'.format(rack))

    # Get and return the Machines

    machines = Machine.query('rack_id', rack.id)

    for machine in machines:
        machine.read()

    return Response(
        200,
        'Successfully retrieved Machines for {}.'.format(rack),
        [x.to_JSON() for x in machines]
    )


def POST(request):
    """Add a Machine to this rack."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'tileId': 'int'
            },
            body={
                'machine': {
                    'rackId': 'int',
                    'position': 'int',
                    'tags': 'list-string',
                    'cpuIds': 'list-int',
                    'gpuIds': 'list-int',
                    'memoryIds': 'list-int',
                    'storageIds': 'list-int'
                }
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Rack from the database

    rack = Rack.from_tile_id(request.params_path['tileId'])

    # Make sure this Rack exists

    if not rack.exists():
        return Response(404, '{} not found.'.format(rack))

    # Make sure this Rack's ID matches the given rack ID

    if rack.id != request.params_body['machine']['rackId']:
        return Response(400, 'Rack ID in `machine` and path do not match.')

    # Make sure this user is authorized to edit this Rack's Machines

    if not rack.google_id_has_at_least(request.google_id, 'VIEW'):
        return Response(403, 'Forbidden from viewing {}.'.format(rack))

    # Instantiate a Machine

    machine = Machine.from_JSON(request.params_body['machine'])

    # Try to insert this Machine

    try:
        machine.insert()

    except exceptions.ForeignKeyError:
        return Response(409, 'Rack position occupied.')

    except:
        return Response(400, 'Invalid Machine.')

    # Return this Machine

    machine.read()

    return Response(
        200,
        'Successfully added {}.'.format(machine),
        machine.to_JSON()
    )
