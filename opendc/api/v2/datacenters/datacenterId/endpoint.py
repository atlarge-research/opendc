from opendc.models.datacenter import Datacenter
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Datacenter."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'datacenterId': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Datacenter from the database

    datacenter = Datacenter.from_primary_key((request.params_path['datacenterId'],))

    # Make sure this Datacenter exists

    if not datacenter.exists():
        return Response(404, '{} not found.'.format(datacenter))

    # Make sure this user is authorized to view this Datacenter

    if not datacenter.google_id_has_at_least(request.google_id, 'VIEW'):
        return Response(403, 'Forbidden from retrieving {}.'.format(datacenter))

    # Return this Datacenter

    datacenter.read()

    return Response(
        200,
        'Successfully retrieved {}.'.format(datacenter),
        datacenter.to_JSON()
    )
