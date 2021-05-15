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

from opendc.models.scenario import Scenario as ScenarioModel, ScenarioSchema
from opendc.models.portfolio import Portfolio
from opendc.exts import current_user, requires_auth


class Scenario(Resource):
    """
    A Scenario resource.
    """
    method_decorators = [requires_auth]

    def get(self, scenario_id):
        """Get scenario by identifier."""
        scenario = ScenarioModel.from_id(scenario_id)
        scenario.check_exists()
        scenario.check_user_access(current_user['sub'], False)
        data = scenario.obj
        return {'data': data}

    def put(self, scenario_id):
        """Update this Scenarios name."""
        schema = Scenario.PutSchema()
        result = schema.load(request.json)

        scenario = ScenarioModel.from_id(scenario_id)

        scenario.check_exists()
        scenario.check_user_access(current_user['sub'], True)

        scenario.set_property('name', result['scenario']['name'])

        scenario.update()
        data = scenario.obj
        return {'data': data}

    def delete(self, scenario_id):
        """Delete this Scenario."""
        scenario = ScenarioModel.from_id(scenario_id)
        scenario.check_exists()
        scenario.check_user_access(current_user['sub'], True)

        scenario_id = scenario.get_id()

        portfolio = Portfolio.from_id(scenario.obj['portfolioId'])
        portfolio.check_exists()
        if scenario_id in portfolio.obj['scenarioIds']:
            portfolio.obj['scenarioIds'].remove(scenario_id)
        portfolio.update()

        old_object = scenario.delete()
        return {'data': old_object}

    class PutSchema(Schema):
        """
        Schema for the put operation.
        """
        scenario = fields.Nested(ScenarioSchema, required=True)
