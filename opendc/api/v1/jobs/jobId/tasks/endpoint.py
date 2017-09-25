from opendc.models.job import Job
from opendc.models.task import Task
from opendc.util import exceptions
from opendc.util.rest import Response


def GET(request):
    """Get this Job's Tasks."""

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

    # Get and return the Tasks

    tasks = Task.query('job_id', request.params_path['jobId'])

    return Response(
        200,
        'Successfully retrieved Tasks for {}.'.format(job),
        [x.to_JSON() for x in tasks]
    )
