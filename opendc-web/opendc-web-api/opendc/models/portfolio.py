from bson import ObjectId
from marshmallow import Schema, fields

from opendc.exts import db
from opendc.models.project import Project
from opendc.models.model import Model


class TargetSchema(Schema):
    """
    Schema representing a target.
    """
    enabledMetrics = fields.List(fields.String())
    repeatsPerScenario = fields.Integer(required=True)


class PortfolioSchema(Schema):
    """
    Schema representing a portfolio.
    """
    _id = fields.String(dump_only=True)
    projectId = fields.String()
    name = fields.String(required=True)
    scenarioIds = fields.List(fields.String())
    targets = fields.Nested(TargetSchema)


class Portfolio(Model):
    """Model representing a Portfolio."""

    collection_name = 'portfolios'

    def check_user_access(self, user_id, edit_access):
        """Raises an error if the user with given [user_id] has insufficient access.

        Checks access on the parent project.

        :param user_id: The User ID of the user.
        :param edit_access: True when edit access should be checked, otherwise view access.
        """
        project = Project.from_id(self.obj['projectId'])
        project.check_user_access(user_id, edit_access)

    @classmethod
    def get_for_project(cls, project_id):
        """Get all portfolios for the specified project id."""
        return db.fetch_all({'projectId': ObjectId(project_id)}, cls.collection_name)
