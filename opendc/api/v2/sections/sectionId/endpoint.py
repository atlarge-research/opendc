from opendc.models.section import Section
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Path's Sections."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'sectionId': 'int'
            }
        )
    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Section from the database

    section = Section.from_primary_key((request.params_path['sectionId'],))

    # Make sure this Section exists

    if not section.exists():
        return Response(404, '{} not found.'.format(section))

    # Make sure this user is authorized to view this Section

    if not section.google_id_has_at_least(request.google_id, 'VIEW'):
        return Response(403, 'Forbidden from viewing {}.'.format(section))

    # Return the Section

    section.read()

    return Response(
        200,
        'Successfully retrieved {}.'.format(section),
        section.to_JSON()
    )
