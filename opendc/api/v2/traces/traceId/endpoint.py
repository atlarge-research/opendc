from opendc.models.trace import Trace
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Trace."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'traceId': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Trace and make sure it exists

    trace = Trace.from_primary_key((request.params_path['traceId'],))

    if not trace.exists():
        return Response(404, '{} not found.'.format(trace))

    # Return this Trace

    return Response(
        200,
        'Successfully retrieved {}.'.format(trace),
        trace.to_JSON()
    )
