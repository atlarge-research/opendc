from opendc.models.model import Model
from opendc.util.database import DB
from opendc.util.exceptions import ClientError
from opendc.util.rest import Response


class User(Model):
    """Model representing a User."""

    collection_name = 'users'

    @classmethod
    def from_email(cls, email):
        """Fetches the user with given email from the collection."""
        return User(DB.fetch_one({'email': email}, User.collection_name))

    @classmethod
    def from_google_id(cls, google_id):
        """Fetches the user with given Google ID from the collection."""
        return User(DB.fetch_one({'googleId': google_id}, User.collection_name))

    def check_correct_user(self, request_google_id):
        """Raises an error if a user tries to modify another user.

        :param request_google_id:
        """
        if request_google_id is not None and self.obj['googleId'] != request_google_id:
            raise ClientError(Response(403, f'Forbidden from editing user with ID {self.obj["_id"]}.'))

    def check_already_exists(self):
        """Checks if the user already exists in the database."""

        existing_user = DB.fetch_one({'googleId': self.obj['googleId']}, self.collection_name)

        if existing_user is not None:
            raise ClientError(Response(409, 'User already exists.'))
