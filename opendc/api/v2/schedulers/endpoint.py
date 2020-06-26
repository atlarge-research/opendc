from opendc.util.rest import Response


SCHEDULERS = ['DEFAULT']


def GET(_):
    """Get all available Schedulers."""

    return Response(200, 'Successfully retrieved Schedulers.', [{'name': name} for name in SCHEDULERS])
