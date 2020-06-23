from opendc.models.scheduler import Scheduler
from opendc.util.rest import Response


def GET(request):
    """Get all available Schedulers."""

    # Get the Schedulers

    schedulers = Scheduler.query()

    # Return the Schedulers

    return Response(
        200,
        'Successfully retrieved Schedulers.',
        [x.to_JSON() for x in schedulers]
    )
