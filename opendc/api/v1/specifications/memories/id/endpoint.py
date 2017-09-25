from opendc.models.memory import Memory
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get the specs of a Memory."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'id': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Memory and make sure it exists

    memory = Memory.from_primary_key((request.params_path['id'],))

    if not memory.exists():
        return Response(404, '{} not found.'.format(memory))

    # Return this Memory

    return Response(
        200,
        'Successfully retrieved {}.'.format(memory),
        memory.to_JSON()
    )
