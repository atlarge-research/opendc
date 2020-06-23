from opendc.models.cpu import CPU
from opendc.util.rest import Response


def GET(request):
    """Get a list of the specifications of all CPUs."""

    # Get the CPUs

    cpus = CPU.query()

    # Return the CPUs

    return Response(
        200,
        'Successfully retrieved CPUs.',
        [x.to_JSON() for x in cpus]
    )
