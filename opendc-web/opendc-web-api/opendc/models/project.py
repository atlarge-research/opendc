from marshmallow import Schema, fields, validate
from werkzeug.exceptions import Forbidden

from opendc.models.model import Model
from opendc.exts import db


class ProjectAuthorizations(Schema):
    """
    Schema representing a project authorization.
    """
    userId = fields.String(required=True)
    level = fields.String(required=True, validate=validate.OneOf(["VIEW", "EDIT", "OWN"]))


class ProjectSchema(Schema):
    """
    Schema representing a Project.
    """
    _id = fields.String(dump_only=True)
    name = fields.String(required=True)
    datetimeCreated = fields.DateTime()
    datetimeLastEdited = fields.DateTime()
    topologyIds = fields.List(fields.String())
    portfolioIds = fields.List(fields.String())
    authorizations = fields.List(fields.Nested(ProjectAuthorizations))


class Project(Model):
    """Model representing a Project."""

    collection_name = 'projects'

    def check_user_access(self, user_id, edit_access):
        """Raises an error if the user with given [user_id] has insufficient access.

        :param user_id: The User ID of the user.
        :param edit_access: True when edit access should be checked, otherwise view access.
        """
        for authorization in self.obj['authorizations']:
            if user_id == authorization['userId'] and authorization['level'] != 'VIEW' or not edit_access:
                return
        raise Forbidden("Forbidden from retrieving project.")

    @classmethod
    def get_for_user(cls, user_id):
        """Get all projects for the specified user id."""
        return db.fetch_all({'authorizations.userId': user_id}, Project.collection_name)
