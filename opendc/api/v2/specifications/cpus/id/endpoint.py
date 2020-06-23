from opendc.models.cpu import CPU
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get the specs of a CPU."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'id': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a CPU and make sure it exists

    cpu = CPU.from_primary_key((request.params_path['id'],))

    if not cpu.exists():
        return Response(404, '{} not found.'.format(cpu))

    # Return this CPU

    return Response(
        200,
        'Successfully retrieved {}.'.format(cpu),
        cpu.to_JSON()
    )
