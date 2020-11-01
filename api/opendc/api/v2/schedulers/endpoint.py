from opendc.util.rest import Response

SCHEDULERS = [
    'mem',
    'mem-inv',
    'core-mem',
    'core-mem-inv',
    'active-servers',
    'active-server-inv',
    'provisioned-cores',
    'provisioned-cores-inv',
    'random'
]


def GET(_):
    """Get all available Schedulers."""

    return Response(200, 'Successfully retrieved Schedulers.', [{'name': name} for name in SCHEDULERS])
