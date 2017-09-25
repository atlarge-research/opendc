from opendc.models.machine import Machine
from opendc.models.rack import Rack
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get the Machine at this location in this Rack."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'tileId': 'int',
                'position': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Machine from the database

    machine = Machine.from_tile_id_and_rack_position(request.params_path['tileId'], request.params_path['position'])

    # Make sure this Machine exists

    if not machine.exists():
        return Response(404, '{} not found.'.format(machine))

    # Make sure this user is authorized to view this Machine

    if not machine.google_id_has_at_least(request.google_id, 'VIEW'):
        return Response(403, 'Forbidden from retrieving {}.'.format(machine))

    # Return this Machine

    machine.read()

    return Response(
        200,
        'Successfully retrieved {}.'.format(machine),
        machine.to_JSON()
    )


def PUT(request):
    """Update the Machine at this location in this Rack."""

    try:
        request.check_required_parameters(
            path={
                'tileId': 'int',
                'position': 'int'
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

    # Instantiate a Machine from the database

    machine = Machine.from_tile_id_and_rack_position(request.params_path['tileId'], request.params_path['position'])

    # Make sure this Machine exists

    if not machine.exists():
        return Response(404, '{} not found.'.format(machine))

    # Make sure this Machine's rack ID is right

    rack = Rack.from_tile_id(request.params_path['tileId'])

    if not rack.exists() or rack.id != request.params_body['machine']['rackId']:
        return Response(400, 'Mismatch in Rack IDs.')

    # Make sure this user is authorized to edit this Machine

    if not machine.google_id_has_at_least(request.google_id, 'EDIT'):
        return Response(403, 'Forbidden from retrieving {}.'.format(machine))

    # Update this Machine

    machine.position = request.params_body['machine']['position']
    machine.tags = request.params_body['machine']['tags']
    machine.cpu_ids = request.params_body['machine']['cpuIds']
    machine.gpu_ids = request.params_body['machine']['gpuIds']
    machine.memory_ids = request.params_body['machine']['memoryIds']
    machine.storage_ids = request.params_body['machine']['storageIds']

    try:
        machine.update()

    except exceptions.ForeignKeyError:
        return Response(409, 'Rack position occupied.')

    except Exception as e:
        print e
        return Response(400, 'Invalid Machine.')

    # Return this Machine

    machine.read()

    return Response(
        200,
        'Successfully updated {}.'.format(machine),
        machine.to_JSON()
    )


def DELETE(request):
    """Delete the Machine at this location in this Rack."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'tileId': 'int',
                'position': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Machine from the database

    machine = Machine.from_tile_id_and_rack_position(request.params_path['tileId'], request.params_path['position'])

    # Make sure this Machine exists

    if not machine.exists():
        return Response(404, '{} not found.'.format(machine))

    # Make sure this user is authorized to edit this Machine

    if not machine.google_id_has_at_least(request.google_id, 'EDIT'):
        return Response(403, 'Forbidden from retrieving {}.'.format(machine))

    # Delete this Machine

    machine.delete()

    # Return this Machine

    return Response(
        200,
        'Successfully deleted {}.'.format(machine),
        machine.to_JSON()
    )
