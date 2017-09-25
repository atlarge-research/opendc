from opendc.models.tile import Tile
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Tile."""

    # Make sure request parameters are there

    try:
        request.check_required_parameters(
            path={
                'tileId': 'int'
            }
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
        return Response(403, 'Forbidden from retrieving {}.'.format(tile))

    # Return this Tile

    tile.read()

    return Response(
        200,
        'Successfully retrieved {}.'.format(tile),
        tile.to_JSON()
    )


def DELETE(request):
    """Delete this Tile."""

    # Make sure request parameters are there

    try:
        request.check_required_parameters(
            path={
                'tileId': 'int'
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
        return Response(403, 'Forbidden from deleting {}.'.format(tile))

    # Delete this Tile

    tile.delete()

    # Return this Tile

    return Response(
        200,
        'Successfully deleted {}.'.format(tile),
        tile.to_JSON()
    )
