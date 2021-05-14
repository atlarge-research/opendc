from opendc.models.model import Model
from opendc.util.exceptions import ClientError
from opendc.util.rest import Response


class Prefab(Model):
    """Model representing a Project."""

    collection_name = 'prefabs'

    def check_user_access(self, user_id):
        """Raises an error if the user with given [user_id] has insufficient access to view this prefab.

        :param user_id: The Google ID of the user.
        """
        if self.obj['authorId'] != user_id and self.obj['visibility'] == "private":
            raise ClientError(Response(403, "Forbidden from retrieving prefab."))
