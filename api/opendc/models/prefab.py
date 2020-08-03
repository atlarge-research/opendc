from opendc.models.model import Model
from opendc.models.user import User
from opendc.util.database import DB
from opendc.util.exceptions import ClientError
from opendc.util.rest import Response


class Prefab(Model):
    """Model representing a Project."""

    collection_name = 'prefabs'

    def check_user_access(self, google_id):
        """Raises an error if the user with given [google_id] has insufficient access to view this prefab.

        :param google_id: The Google ID of the user.
        """
        user = User.from_google_id(google_id)

        # TODO(Jacob) add special handling for OpenDC-provided prefabs

        #try:

        print(self.obj)
        if self.obj['authorId'] != user.get_id() and self.obj['visibility'] == "private":
            raise ClientError(Response(403, "Forbidden from retrieving prefab."))
        #except KeyError:
            # OpenDC-authored objects don't necessarily have an authorId
        #    return


def query_all(query):
    """Returns a list of all prefabs matching the query.

    :param query: the query to execute on the db.
    """
    return DB.fetch_all(query, Prefab.collection_name)
