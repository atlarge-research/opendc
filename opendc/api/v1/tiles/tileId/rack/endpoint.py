from opendc.models.rack import Rack
from opendc.models.tile import Tile
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Tile's Rack."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'tileId': 'int'
            },
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Tile from the database

    tile = Tile.from_primary_key((request.params_path['tileId'],))

    # Make sure this Tile exists

    if not tile.exists():
        return Response(404, '{} not found.'.format(tile))

    # Make sure this user is authorized to view this Tile

    if not tile.google_id_has_at_least(request.google_id, 'VIEW'):
        return Response(403, 'Forbidden from editing {}'.format(tile))

    # Instantiate a Rack from the database

    rack = Rack.from_primary_key((tile.object_id,))

    # Make sure this Rack exists

    if not rack.exists():
        return Response(404, '{} not found'.format(rack))

    # Return the Rack

    rack.read()

    return Response(
        200,
        'Successfully retrieved {}.'.format(rack),
        rack.to_JSON()
    )


def POST(request):
    """Add a Rack to this Tile if it is empty."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'tileId': 'int'
            },
            body={
                'rack': {
                    'name': 'string',
                    'capacity': 'int',
                    'powerCapacityW': 'int'
                }
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Tile from the database

    tile = Tile.from_primary_key((request.params_path['tileId'],))

    # Make sure this Tile exists

    if not tile.exists():
        return Response(404, '{} not found.'.format(tile))

    # Make sure this user is authorized to edit this Tile

    if not tile.google_id_has_at_least(request.google_id, 'EDIT'):
        return Response(403, 'Forbidden from editing {}'.format(tile))

    # Make sure this Tile isn't occupied

    if tile.object_id is not None:
        return Response(409, '{} occupied.'.format(tile))

    # Instantiate a Rack and insert it into the database

    rack = Rack.from_JSON(request.params_body['rack'])
    rack.insert()

    # Try to add this Rack to this Tile

    tile.object_id = rack.id
    tile.object_type = 'RACK'
    tile.update()

    # Return this Rack

    rack.read()

    return Response(
        200,
        'Successfully added {}.'.format(rack),
        rack.to_JSON()
    )


def PUT(request):
    """Update the Rack on this Tile."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'tileId': 'int'
            },
            body={
                'rack': {
                    'name': 'string',
                    'capacity': 'int',
                    'powerCapacityW': 'int'
                }
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Tile from the database

    tile = Tile.from_primary_key((request.params_path['tileId'],))

    # Make sure this Tile exists

    if not tile.exists():
        return Response(404, '{} not found.'.format(tile))

    # Make sure this user is authorized to edit this Tile

    if not tile.google_id_has_at_least(request.google_id, 'EDIT'):
        return Response(403, 'Forbidden from editing {}'.format(tile))

    # Instantiate a Rack from the database

    rack = Rack.from_primary_key((tile.object_id,))

    # Make sure this Rack exists

    if not rack.exists():
        return Response(404, '{} not found'.format(rack))

    # Update this Rack

    rack.name = request.params_body['rack']['name']
    rack.capacity = request.params_body['rack']['capacity']

    rack.update()

    # Return this Rack

    rack.read()

    return Response(
        200,
        'Successfully updated {}.'.format(rack),
        rack.to_JSON()
    )


def DELETE(request):
    """Delete this Tile's Rack."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'tileId': 'int'
            },
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Tile from the database

    tile = Tile.from_primary_key((request.params_path['tileId'],))

    # Make sure this Tile exists

    if not tile.exists():
        return Response(404, '{} not found.'.format(tile))

    # Make sure this user is authorized to edit this Tile

    if not tile.google_id_has_at_least(request.google_id, 'EDIT'):
        return Response(403, 'Forbidden from editing {}'.format(tile))

    # Instantiate a Rack from the database

    rack = Rack.from_primary_key((tile.object_id,))

    # Make sure this Rack exists

    if not rack.exists():
        return Response(404, '{} not found'.format(rack))

    # Remove this Rack from this Tile

    tile.object_id = None
    tile.object_type = None

    tile.update()

    # Delete this Rack

    rack.delete()

    # Return this Rack

    return Response(
        200,
        'Successfully deleted {}.'.format(rack),
        rack.to_JSON()
    )
