from opendc.models.gpu import GPU
from opendc.util.rest import Response


def GET(request):
    """Get a list of the specifications of all GPUs."""

    # Get the GPUs

    gpus = GPU.query()

    # Return the GPUs

    return Response(
        200,
        'Successfully retrieved GPUs.',
        [x.to_JSON() for x in gpus]
    )
