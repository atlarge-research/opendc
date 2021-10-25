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
from marshmallow import Schema, fields

from opendc.exts import requires_auth, current_user, has_scope
from opendc.models.portfolio import Portfolio as PortfolioModel, PortfolioSchema
from opendc.models.project import Project
from opendc.models.scenario import ScenarioSchema, Scenario
from opendc.models.topology import Topology


class Portfolio(Resource):
    """
    Resource representing a portfolio.
    """
    method_decorators = [requires_auth]

    def get(self, portfolio_id):
        """
        Get a portfolio by identifier.
        """
        portfolio = PortfolioModel.from_id(portfolio_id)

        portfolio.check_exists()

        # Users with scope runner can access all portfolios
        if not has_scope('runner'):
            portfolio.check_user_access(current_user['sub'], False)

        data = PortfolioSchema().dump(portfolio.obj)
        return {'data': data}

    def put(self, portfolio_id):
        """
        Replace the portfolio.
        """
        schema = Portfolio.PutSchema()
        result = schema.load(request.json)

        portfolio = PortfolioModel.from_id(portfolio_id)
        portfolio.check_exists()
        portfolio.check_user_access(current_user['sub'], True)

        portfolio.set_property('name', result['portfolio']['name'])
        portfolio.set_property('targets.enabledMetrics', result['portfolio']['targets']['enabledMetrics'])
        portfolio.set_property('targets.repeatsPerScenario', result['portfolio']['targets']['repeatsPerScenario'])

        portfolio.update()
        data = PortfolioSchema().dump(portfolio.obj)
        return {'data': data}

    def delete(self, portfolio_id):
        """
        Delete a portfolio.
        """
        portfolio = PortfolioModel.from_id(portfolio_id)

        portfolio.check_exists()
        portfolio.check_user_access(current_user['sub'], True)

        portfolio_id = portfolio.get_id()

        project = Project.from_id(portfolio.obj['projectId'])
        project.check_exists()
        if portfolio_id in project.obj['portfolioIds']:
            project.obj['portfolioIds'].remove(portfolio_id)
        project.update()

        old_object = portfolio.delete()
        data = PortfolioSchema().dump(old_object)
        return {'data': data}

    class PutSchema(Schema):
        """
        Schema for the PUT operation on a portfolio.
        """
        portfolio = fields.Nested(PortfolioSchema, required=True)


class PortfolioScenarios(Resource):
    """
    Resource representing the scenarios of a portfolio.
    """
    method_decorators = [requires_auth]

    def get(self, portfolio_id):
        """
        Get all scenarios belonging to a portfolio.
        """
        portfolio = PortfolioModel.from_id(portfolio_id)

        portfolio.check_exists()
        portfolio.check_user_access(current_user['sub'], True)

        scenarios = Scenario.get_for_portfolio(portfolio_id)

        data = ScenarioSchema().dump(scenarios, many=True)
        return {'data': data}

    def post(self, portfolio_id):
        """
        Add a new scenario to this portfolio
        """
        schema = PortfolioScenarios.PostSchema()
        result = schema.load(request.json)

        portfolio = PortfolioModel.from_id(portfolio_id)

        portfolio.check_exists()
        portfolio.check_user_access(current_user['sub'], True)

        scenario = Scenario(result['scenario'])

        topology = Topology.from_id(scenario.obj['topology']['topologyId'])
        topology.check_exists()
        topology.check_user_access(current_user['sub'], True)

        scenario.set_property('portfolioId', portfolio.get_id())
        scenario.set_property('simulation', {'state': 'QUEUED'})
        scenario.set_property('topology.topologyId', topology.get_id())

        scenario.insert()

        portfolio.obj['scenarioIds'].append(scenario.get_id())
        portfolio.update()
        data = ScenarioSchema().dump(scenario.obj)
        return {'data': data}

    class PostSchema(Schema):
        """
        Schema for the POST operation on a portfolio's scenarios.
        """
        scenario = fields.Nested(ScenarioSchema, required=True)
