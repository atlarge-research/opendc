from opendc.models.room_type import RoomType
from opendc.util.rest import Response


def GET(request):
    """Get all available room types."""

    # Get the RoomTypes

    room_types = RoomType.query()

    # Return the RoomTypes

    return Response(
        200,
        'Successfully retrieved RoomTypes.',
        [x.to_JSON() for x in room_types]
    )
