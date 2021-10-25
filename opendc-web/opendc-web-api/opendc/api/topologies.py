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

from datetime import datetime

from flask import request
from flask_restful import Resource
from marshmallow import Schema, fields

from opendc.models.project import Project
from opendc.models.topology import Topology as TopologyModel, TopologySchema
from opendc.exts import current_user, requires_auth, has_scope


class Topology(Resource):
    """
    Resource representing a single topology.
    """
    method_decorators = [requires_auth]

    def get(self, topology_id):
        """
        Get a single topology.
        """
        topology = TopologyModel.from_id(topology_id)
        topology.check_exists()

        # Users with scope runner can access all topologies
        if not has_scope('runner'):
            topology.check_user_access(current_user['sub'], False)

        data = TopologySchema().dump(topology.obj)
        return {'data': data}

    def put(self, topology_id):
        """
        Replace the topology.
        """
        topology = TopologyModel.from_id(topology_id)

        schema = Topology.PutSchema()
        result = schema.load(request.json)

        topology.check_exists()
        topology.check_user_access(current_user['sub'], True)

        topology.set_property('name', result['topology']['name'])
        topology.set_property('rooms', result['topology']['rooms'])
        topology.set_property('datetimeLastEdited', datetime.now())

        topology.update()
        data = TopologySchema().dump(topology.obj)
        return {'data': data}

    def delete(self, topology_id):
        """
        Delete a topology.
        """
        topology = TopologyModel.from_id(topology_id)

        topology.check_exists()
        topology.check_user_access(current_user['sub'], True)

        topology_id = topology.get_id()

        project = Project.from_id(topology.obj['projectId'])
        project.check_exists()
        if topology_id in project.obj['topologyIds']:
            project.obj['topologyIds'].remove(topology_id)
        project.update()

        old_object = topology.delete()
        data = TopologySchema().dump(old_object)
        return {'data': data}

    class PutSchema(Schema):
        """
        Schema for the PUT operation on a topology.
        """
        topology = fields.Nested(TopologySchema, required=True)
