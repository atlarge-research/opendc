from opendc.models.gpu import GPU
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get the specs of a GPU."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'id': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a GPU and make sure it exists

    gpu = GPU.from_primary_key((request.params_path['id'],))

    if not gpu.exists():
        return Response(404, '{} not found.'.format(gpu))

    # Return this GPU

    return Response(
        200,
        'Successfully retrieved {}.'.format(gpu),
        gpu.to_JSON()
    )
