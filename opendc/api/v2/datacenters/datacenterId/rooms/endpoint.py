from opendc.models.datacenter import Datacenter
from opendc.models.room import Room
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Datacenter's Rooms."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'datacenterId': 'int'
            }
        )
    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Datacenter from the database

    datacenter = Datacenter.from_primary_key((request.params_path['datacenterId'],))

    # Make sure this Datacenter exists

    if not datacenter.exists():
        return Response(404, '{} not found.'.format(datacenter))

    # Make sure this user is authorized to view this Datacenter's Rooms

    if not datacenter.google_id_has_at_least(request.google_id, 'VIEW'):
        return Response(403, 'Forbidden from viewing Rooms for {}.'.format(datacenter))

    # Get and return the Rooms

    rooms = Room.query('datacenter_id', datacenter.id)

    return Response(
        200,
        'Successfully retrieved Rooms for {}.'.format(datacenter),
        [x.to_JSON() for x in rooms]
    )


def POST(request):
    """Add a Room."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'datacenterId': 'int'
            },
            body={
                'room': {
                    'id': 'int',
                    'datacenterId': 'int',
                    'roomType': 'string'
                }
            }
        )
    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Make sure the passed object's datacenter id matches the path datacenter id

    if request.params_path['datacenterId'] != request.params_body['room']['datacenterId']:
        return Response(400, 'ID mismatch.')

    # Instantiate a Datacenter from the database

    datacenter = Datacenter.from_primary_key((request.params_path['datacenterId'],))

    # Make sure this Datacenter exists

    if not datacenter.exists():
        return Response(404, '{} not found.'.format(datacenter))

    # Make sure this user is authorized to edit this Datacenter's Rooms

    if not datacenter.google_id_has_at_least(request.google_id, 'EDIT'):
        return Response(403, 'Forbidden from adding a Room to {}.'.format(datacenter))

    # Add a name if not provided

    if 'name' not in request.params_body['room']:
        room_count = len(Room.query('datacenter_id', datacenter.id))
        request.params_body['room']['name'] = 'Room {}'.format(room_count)

    # Instantiate a Room

    room = Room.from_JSON(request.params_body['room'])

    # Try to insert this Room

    try:
        room.insert()
    except:
        return Response(400, 'Invalid `roomType` or existing `name`.')

    # Return this Room

    room.read()

    return Response(
        200,
        'Successfully added {}.'.format(room),
        room.to_JSON()
    )
