from opendc.models.model import Model
from opendc.util.database import DB
from opendc.util.exceptions import ClientError
from opendc.util.rest import Response


class Project(Model):
    """Model representing a Project."""

    collection_name = 'projects'

    def check_user_access(self, user_id, edit_access):
        """Raises an error if the user with given [user_id] has insufficient access.

        :param user_id: The User ID of the user.
        :param edit_access: True when edit access should be checked, otherwise view access.
        """
        for authorization in self.obj['authorizations']:
            if user_id == authorization['userId'] and authorization['authorizationLevel'] != 'VIEW' or not edit_access:
                return
        raise ClientError(Response(403, "Forbidden from retrieving project."))

    @classmethod
    def get_for_user(cls, user_id):
        """Get all projects for the specified user id."""
        return DB.fetch_all({'authorizations': {
            'userId': user_id
        }}, Project.collection_name)
