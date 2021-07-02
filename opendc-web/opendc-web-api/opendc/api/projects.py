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

from opendc.models.portfolio import Portfolio, PortfolioSchema
from opendc.models.topology import Topology, TopologySchema
from opendc.models.project import Project as ProjectModel, ProjectSchema
from opendc.exts import current_user, requires_auth


class ProjectList(Resource):
    """
    Resource representing the list of projects available to a user.
    """
    method_decorators = [requires_auth]

    def get(self):
        """Get the authorized projects of the user"""
        user_id = current_user['sub']
        projects = ProjectModel.get_for_user(user_id)
        data = ProjectSchema().dump(projects, many=True)
        return {'data': data}

    def post(self):
        """Create a new project, and return that new project."""
        user_id = current_user['sub']

        schema = Project.PutSchema()
        result = schema.load(request.json)

        topology = Topology({'name': 'Default topology', 'rooms': []})
        topology.insert()

        project = ProjectModel(result['project'])
        project.set_property('datetimeCreated', datetime.now())
        project.set_property('datetimeLastEdited', datetime.now())
        project.set_property('topologyIds', [topology.get_id()])
        project.set_property('portfolioIds', [])
        project.set_property('authorizations', [{'userId': user_id, 'level': 'OWN'}])
        project.insert()

        topology.set_property('projectId', project.get_id())
        topology.update()

        data = ProjectSchema().dump(project.obj)
        return {'data': data}


class Project(Resource):
    """
    Resource representing a single project.
    """
    method_decorators = [requires_auth]

    def get(self, project_id):
        """Get this Project."""
        project = ProjectModel.from_id(project_id)

        project.check_exists()
        project.check_user_access(current_user['sub'], False)

        data = ProjectSchema().dump(project.obj)
        return {'data': data}

    def put(self, project_id):
        """Update a project's name."""
        schema = Project.PutSchema()
        result = schema.load(request.json)

        project = ProjectModel.from_id(project_id)

        project.check_exists()
        project.check_user_access(current_user['sub'], True)

        project.set_property('name', result['project']['name'])
        project.set_property('datetimeLastEdited', datetime.now())
        project.update()

        data = ProjectSchema().dump(project.obj)
        return {'data': data}

    def delete(self, project_id):
        """Delete this Project."""
        project = ProjectModel.from_id(project_id)

        project.check_exists()
        project.check_user_access(current_user['sub'], True)

        for topology_id in project.obj['topologyIds']:
            topology = Topology.from_id(topology_id)
            topology.delete()

        for portfolio_id in project.obj['portfolioIds']:
            portfolio = Portfolio.from_id(portfolio_id)
            portfolio.delete()

        old_object = project.delete()
        data = ProjectSchema().dump(old_object)
        return {'data': data}

    class PutSchema(Schema):
        """
        Schema for the PUT operation on a project.
        """
        project = fields.Nested(ProjectSchema, required=True)


class ProjectTopologies(Resource):
    """
    Resource representing the topologies of a project.
    """
    method_decorators = [requires_auth]

    def post(self, project_id):
        """Add a new Topology to the specified project and return it"""
        schema = ProjectTopologies.PutSchema()
        result = schema.load(request.json)

        project = ProjectModel.from_id(project_id)

        project.check_exists()
        project.check_user_access(current_user['sub'], True)

        topology = Topology({
            'projectId': project.get_id(),
            'name': result['topology']['name'],
            'rooms': result['topology']['rooms'],
        })

        topology.insert()

        project.obj['topologyIds'].append(topology.get_id())
        project.set_property('datetimeLastEdited', datetime.now())
        project.update()

        data = TopologySchema().dump(topology.obj)
        return {'data': data}

    class PutSchema(Schema):
        """
        Schema for the PUT operation on a project topology.
        """
        topology = fields.Nested(TopologySchema, required=True)


class ProjectPortfolios(Resource):
    """
    Resource representing the portfolios of a project.
    """
    method_decorators = [requires_auth]

    def post(self, project_id):
        """Add a new Portfolio for this Project."""
        schema = ProjectPortfolios.PutSchema()
        result = schema.load(request.json)

        project = ProjectModel.from_id(project_id)

        project.check_exists()
        project.check_user_access(current_user['sub'], True)

        portfolio = Portfolio(result['portfolio'])

        portfolio.set_property('projectId', project.get_id())
        portfolio.set_property('scenarioIds', [])

        portfolio.insert()

        project.obj['portfolioIds'].append(portfolio.get_id())
        project.update()

        return {'data': portfolio.obj}

    class PutSchema(Schema):
        """
        Schema for the PUT operation on a project portfolio.
        """
        portfolio = fields.Nested(PortfolioSchema, required=True)
