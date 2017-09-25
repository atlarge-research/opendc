from opendc.models.path import Path
from opendc.models.section import Section
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Path's Sections."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'pathId': 'int'
            }
        )
    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Path from the database

    path = Path.from_primary_key((request.params_path['pathId'],))

    # Make sure this Path exists

    if not path.exists():
        return Response(404, '{} not found.'.format(path))

    # Make sure this user is authorized to view this Path's Sections

    if not path.google_id_has_at_least(request.google_id, 'VIEW'):
        return Response(403, 'Forbidden from viewing Sections for {}.'.format(path))

    # Get and return the Sections

    sections = Section.query('path_id', request.params_path['pathId'])

    return Response(
        200,
        'Successfully retrieved Sections for {}.'.format(path),
        [x.to_JSON() for x in sections]
    )
