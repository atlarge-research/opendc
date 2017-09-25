from opendc.models.model import Model


class User(Model):
    JSON_TO_PYTHON_DICT = {
        'User': {
            'id': 'id',
            'googleId': 'google_id',
            'email': 'email',
            'givenName': 'given_name',
            'familyName': 'family_name'
        }
    }

    TABLE_NAME = 'users'
    COLUMNS = ['id', 'google_id', 'email', 'given_name', 'family_name']
    COLUMNS_PRIMARY_KEY = ['id']

    @classmethod
    def from_google_id(cls, google_id):
        """Initialize a User by fetching them by their google id."""

        user = cls._from_database('SELECT * FROM users WHERE google_id = %s', (google_id,))

        if user is not None:
            return user

        return User()

    @classmethod
    def from_email(cls, email):
        """Initialize a User by fetching them by their email."""

        user = cls._from_database('SELECT * FROM users WHERE email = %s', (email,))

        if user is not None:
            return user

        return User()

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the User has at least the given auth level over this User."""

        if authorization_level in ['EDIT', 'OWN']:
            return google_id == self.google_id

        return True
