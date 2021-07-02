#  Copyright (c) 2021 AtLarge Research
#
#  Permission is hereby granted, free of charge, to any person obtaining a copy
#  of this software and associated documentation files (the "Software"), to deal
#  in the Software without restriction, including without limitation the rights
#  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
#  copies of the Software, and to permit persons to whom the Software is
#  furnished to do so, subject to the following conditions:
#
#  The above copyright notice and this permission notice shall be included in all
#  copies or substantial portions of the Software.
#
#  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
#  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
#  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
#  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
#  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
#  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
#  SOFTWARE.
from flask import request
from flask_restful import Resource
from marshmallow import fields, Schema, validate
from werkzeug.exceptions import BadRequest, Conflict

from opendc.exts import requires_auth, requires_scope
from opendc.models.scenario import Scenario


def convert_to_job(scenario):
    """Convert a scenario to a job.
    """
    return JobSchema().dump({
        '_id': scenario['_id'],
        'scenarioId': scenario['_id'],
        'state': scenario['simulation']['state'],
        'heartbeat': scenario['simulation'].get('heartbeat', None),
        'results': scenario.get('results', {})
    })


class JobSchema(Schema):
    """
    Schema representing a simulation job.
    """
    _id = fields.String(dump_only=True)
    scenarioId = fields.String(dump_only=True)
    state = fields.String(required=True,
                          validate=validate.OneOf(["QUEUED", "CLAIMED", "RUNNING", "FINISHED", "FAILED"]))
    heartbeat = fields.DateTime()
    results = fields.Dict()


class JobList(Resource):
    """
    Resource representing the list of available jobs.
    """
    method_decorators = [requires_auth, requires_scope('runner')]

    def get(self):
        """Get all available jobs."""
        jobs = Scenario.get_jobs()
        data = list(map(convert_to_job, jobs.obj))
        return {'data': data}


class Job(Resource):
    """
    Resource representing a single job.
    """
    method_decorators = [requires_auth, requires_scope('runner')]

    def get(self, job_id):
        """Get the details of a single job."""
        job = Scenario.from_id(job_id)
        job.check_exists()
        data = convert_to_job(job.obj)
        return {'data': data}

    def post(self, job_id):
        """Update the details of a single job."""
        action = JobSchema(only=('state', 'results')).load(request.json)

        job = Scenario.from_id(job_id)
        job.check_exists()

        old_state = job.obj['simulation']['state']
        new_state = action['state']

        if old_state == new_state:
            data = job.update_state(new_state)
        elif (old_state, new_state) == ('QUEUED', 'CLAIMED'):
            data = job.update_state('CLAIMED')
        elif (old_state, new_state) == ('CLAIMED', 'RUNNING'):
            data = job.update_state('RUNNING')
        elif (old_state, new_state) == ('RUNNING', 'FINISHED'):
            data = job.update_state('FINISHED', results=action.get('results', None))
        elif old_state in ('CLAIMED', 'RUNNING') and new_state == 'FAILED':
            data = job.update_state('FAILED')
        else:
            raise BadRequest('Invalid state transition')

        if not data:
            raise Conflict('State conflict')

        return {'data': convert_to_job(data)}
