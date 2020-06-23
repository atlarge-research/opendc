from opendc.models.failure_model import FailureModel
from opendc.util.rest import Response


def GET(request):
    """Get all Failure Models."""

    # Get the FailureModels

    failure_models = FailureModel.query()

    # Return the FailureModels

    return Response(
        200,
        'Successfully retrieved FailureModels.',
        [x.to_JSON() for x in failure_models]
    )
