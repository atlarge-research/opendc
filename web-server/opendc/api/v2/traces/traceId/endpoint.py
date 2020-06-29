from opendc.models.trace import Trace
from opendc.util.rest import Response


def GET(request):
    """Get this Trace."""

    request.check_required_parameters(path={'traceId': 'string'})

    trace = Trace.from_id(request.params_path['traceId'])

    trace.check_exists()

    return Response(200, f'Successfully retrieved trace.', trace.obj)
