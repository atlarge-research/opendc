from opendc.models.storage import Storage
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get the specs of a Storage."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'id': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Storage and make sure it exists

    storage = Storage.from_primary_key((request.params_path['id'],))

    if not storage.exists():
        return Response(404, '{} not found.'.format(storage))

    # Return this CPU

    return Response(
        200,
        'Successfully retrieved {}.'.format(storage),
        storage.to_JSON()
    )
