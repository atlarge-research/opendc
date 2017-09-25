from opendc.models.memory import Memory
from opendc.util.rest import Response


def GET(request):
    """Get a list of the specifications of all Memories."""

    # Get the Memories

    memories = Memory.query()

    # Return the Memories

    return Response(
        200,
        'Successfully retrieved Memories.',
        [x.to_JSON() for x in memories]
    )
