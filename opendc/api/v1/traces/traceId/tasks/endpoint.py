from opendc.models.trace import Trace
from opendc.models.task import Task
from opendc.util import database, exceptions
from opendc.util.rest import Response

def GET(request):
    """Get this Trace's Tasks."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path = {
                'traceId': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Trace and make sure it exists

    trace = Trace.from_primary_key((request.params_path['traceId'],))

    if not trace.exists():
        return Response(404, '{} not found.'.format(trace))

    # Get and return the Tasks

    tasks = Task.query('trace_id', request.params_path['traceId'])

    return Response(
        200,
        'Successfully retrieved Tasks for {}.'.format(trace),
        [x.to_JSON() for x in tasks]
    )
