from opendc.models.trace import Trace
from opendc.util.rest import Response


def GET(_):
    """Get all available Traces."""

    traces = Trace.get_all()

    return Response(200, 'Successfully retrieved Traces', traces.obj)
