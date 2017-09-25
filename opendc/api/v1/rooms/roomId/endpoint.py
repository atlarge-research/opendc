from opendc.models.room import Room
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Room."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'roomId': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Room from the database

    room = Room.from_primary_key((request.params_path['roomId'],))

    # Make sure this Room exists

    if not room.exists():
        return Response(404, '{} not found.'.format(room))

    # Make sure this user is authorized to view this Room

    if not room.google_id_has_at_least(request.google_id, 'VIEW'):
        return Response(403, 'Forbidden from retrieving {}.'.format(room))

    # Return this Room

    room.read()

    return Response(
        200,
        'Successfully retrieved {}.'.format(room),
        room.to_JSON()
    )


def PUT(request):
    """Update this Room's name and type."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'roomId': 'int'
            },
            body={
                'room': {
                    'name': 'string',
                    'roomType': 'string'
                }
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Room from the database

    room = Room.from_primary_key((request.params_path['roomId'],))

    # Make sure this Room exists

    if not room.exists():
        return Response(404, '{} not found.'.format(room))

    # Make sure this user is authorized to edit this Room

    if not room.google_id_has_at_least(request.google_id, 'EDIT'):
        return Response(403, 'Forbidden from updating {}.'.format(room))

    # Update this Room

    room.name = request.params_body['room']['name']
    room.type = request.params_body['room']['roomType']

    try:
        room.update()
    except exceptions.ForeignKeyError:
        return Response(400, 'Invalid `roomType` or existing `name`.')

    # Return this Room

    return Response(
        200,
        'Successfully updated {}.'.format(room),
        room.to_JSON()
    )


def DELETE(request):
    """Delete this Room."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'roomId': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Room and make sure it exists

    room = Room.from_primary_key((request.params_path['roomId'],))

    if not room.exists():
        return Response(404, '{} not found.'.format(room))

    # Make sure this user is authorized to delete this Room

    if not room.google_id_has_at_least(request.google_id, 'EDIT'):
        return Response(403, 'Forbidden from deleting {}.'.format(room))

    # Delete this Room

    room.delete()

    # Return this Room

    return Response(
        200,
        'Successfully deleted {}.'.format(room),
        room.to_JSON()
    )
