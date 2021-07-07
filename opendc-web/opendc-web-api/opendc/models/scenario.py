from datetime import datetime

from bson import ObjectId
from marshmallow import Schema, fields

from opendc.exts import db
from opendc.models.model import Model
from opendc.models.portfolio import Portfolio


class TraceSchema(Schema):
    """
    Schema for specifying the trace of a scenario.
    """
    traceId = fields.String()
    loadSamplingFraction = fields.Float()


class TopologySchema(Schema):
    """
    Schema for topology specification for a scenario.
    """
    topologyId = fields.String()


class OperationalSchema(Schema):
    """
    Schema for the operational phenomena for a scenario.
    """
    failuresEnabled = fields.Boolean()
    performanceInterferenceEnabled = fields.Boolean()
    schedulerName = fields.String()


class ScenarioSchema(Schema):
    """
    Schema representing a scenario.
    """
    _id = fields.String(dump_only=True)
    portfolioId = fields.String()
    name = fields.String(required=True)
    trace = fields.Nested(TraceSchema)
    topology = fields.Nested(TopologySchema)
    operational = fields.Nested(OperationalSchema)


class Scenario(Model):
    """Model representing a Scenario."""

    collection_name = 'scenarios'

    def check_user_access(self, user_id, edit_access):
        """Raises an error if the user with given [user_id] has insufficient access.

        Checks access on the parent project.

        :param user_id: The User ID of the user.
        :param edit_access: True when edit access should be checked, otherwise view access.
        """
        portfolio = Portfolio.from_id(self.obj['portfolioId'])
        portfolio.check_user_access(user_id, edit_access)

    @classmethod
    def get_jobs(cls):
        """Obtain the scenarios that have been queued.
        """
        return cls(db.fetch_all({'simulation.state': 'QUEUED'}, cls.collection_name))

    @classmethod
    def get_for_portfolio(cls, portfolio_id):
        """Get all scenarios for the specified portfolio id."""
        return db.fetch_all({'portfolioId': ObjectId(portfolio_id)}, cls.collection_name)

    def update_state(self, new_state, results=None):
        """Atomically update the state of the Scenario.
        """
        update = {'$set': {'simulation.state': new_state, 'simulation.heartbeat': datetime.now()}}
        if results:
            update['$set']['results'] = results
        return db.fetch_and_update(
            query={'_id': self.obj['_id'], 'simulation.state': self.obj['simulation']['state']},
            update=update,
            collection=self.collection_name
        )
