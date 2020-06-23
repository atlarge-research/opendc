from opendc.models.job import Job
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Job."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'jobId': 'int'
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate a Job and make sure it exists

    job = Job.from_primary_key((request.params_path['jobId'],))

    if not job.exists():
        return Response(404, '{} not found.'.format(job))

    # Return this Job

    return Response(
        200,
        'Successfully retrieved {}.'.format(job),
        job.to_JSON()
    )
