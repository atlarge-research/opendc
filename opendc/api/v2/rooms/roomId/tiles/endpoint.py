from opendc.models.room import Room
from opendc.models.tile import Tile
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Room's Tiles."""

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

    # Make sure this user is authorized to view this Room's Tiles

    if not room.google_id_has_at_least(request.google_id, 'VIEW'):
        return Response(403, 'Forbidden from viewing Tiles for {}.'.format(room))

    # Get and return the Tiles

    tiles = Tile.query('room_id', room.id)

    for tile in tiles:
        tile.read()

    return Response(
        200,
        'Successfully retrieved Tiles for {}.'.format(room),
        [x.to_JSON() for x in tiles]
    )


def POST(request):
    """Add a Tile."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'roomId': 'int'
            },
            body={
                'tile': {
                    'roomId': 'int',
                    'positionX': 'int',
                    'positionY': 'int'
                }
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    if request.params_path['roomId'] != request.params_body['tile']['roomId']:
        return Response(400, 'ID mismatch')

    # Instantiate a Room from the database

    room = Room.from_primary_key((request.params_path['roomId'],))

    # Make sure this Room exists

    if not room.exists():
        return Response(404, '{} not found.'.format(room))

    # Make sure this user is authorized to edit this Room's Tiles

    if not room.google_id_has_at_least(request.google_id, 'EDIT'):
        return Response(403, 'Forbidden from adding Tiles to {}.'.format(room))

    # Clean the tile JSON

    tile_json = request.params_body['tile']

    tile_json['objectId'] = None
    tile_json['objectType'] = None

    # Instantiate a Tile

    tile = Tile.from_JSON(tile_json)

    # Try to insert this Tile

    try:
        tile.insert()

    except exceptions.ForeignKeyError as e:

        if e.message == 'OccupiedTilePosition':
            return Response(409, 'Tile position occupied.')

        elif e.message == 'InvalidTilePosition':
            return Response(400, 'Invalid Tile position (new Tiles must neighbor existing Tiles).')

    # Return this Tile

    tile.read()

    return Response(
        200,
        'Successfully added {}.'.format(tile),
        tile.to_JSON()
    )
