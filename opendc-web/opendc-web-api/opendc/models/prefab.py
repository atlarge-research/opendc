from marshmallow import Schema, fields
from werkzeug.exceptions import Forbidden

from opendc.models.topology import ObjectSchema
from opendc.models.model import Model


class PrefabSchema(Schema):
    """
    Schema for a Prefab.
    """
    _id = fields.String()
    name = fields.String(required=True)
    datetimeCreated = fields.DateTime()
    datetimeLastEdited = fields.DateTime()
    rack = fields.Nested(ObjectSchema)


class Prefab(Model):
    """Model representing a Prefab."""

    collection_name = 'prefabs'

    def check_user_access(self, user_id):
        """Raises an error if the user with given [user_id] has insufficient access to view this prefab.

        :param user_id: The user ID of the user.
        """
        if self.obj['authorId'] != user_id and self.obj['visibility'] == "private":
            raise Forbidden("Forbidden from retrieving prefab.")
