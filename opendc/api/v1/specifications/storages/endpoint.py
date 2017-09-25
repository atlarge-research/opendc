from opendc.models.storage import Storage
from opendc.util.rest import Response


def GET(request):
    """Get a list of the specifications of all Storages."""

    # Get the Storages

    storages = Storage.query()

    # Return the Storages

    return Response(
        200,
        'Successfully retrieved Storages.',
        [x.to_JSON() for x in storages]
    )
