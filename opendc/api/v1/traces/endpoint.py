from opendc.models.trace import Trace
from opendc.util.rest import Response


def GET(request):
    """Get all available Traces."""

    # Get the Traces

    traces = Trace.query()

    # Return the Traces

    return Response(
        200,
        'Successfully retrieved Traces',
        [x.to_JSON() for x in traces]
    )
