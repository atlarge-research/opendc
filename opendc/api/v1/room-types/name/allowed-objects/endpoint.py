from opendc.models.allowed_object import AllowedObject
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this room's allowed objects."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'name': 'string'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Get the AllowedObjects

    allowed_objects = AllowedObject.query('room_type', request.params_path['name'])

    # Return the AllowedObjects

    return Response(
        200,
        'Successfully retrieved AllowedObjects.',
        [x.to_JSON() for x in allowed_objects]
    )
