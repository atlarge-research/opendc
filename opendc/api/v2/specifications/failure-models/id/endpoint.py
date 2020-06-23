from opendc.models.failure_model import FailureModel
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Failure Model."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'id': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a FailureModel and make sure it exists

    failure_model = FailureModel.from_primary_key((request.params_path['id'],))

    if not failure_model.exists():
        return Response(404, '{} not found.'.format(failure_model))

    # Return this FailureModel

    return Response(
        200,
        'Successfully retrieved {}.'.format(failure_model),
        failure_model.to_JSON()
    )
