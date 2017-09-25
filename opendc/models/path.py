from opendc.models.authorization import Authorization
from opendc.models.model import Model
from opendc.models.user import User
from opendc.util import exceptions


class Path(Model):
    JSON_TO_PYTHON_DICT = {
        'Path': {
            'id': 'id',
            'simulationId': 'simulation_id',
            'name': 'name',
            'datetimeCreated': 'datetime_created'
        }
    }

    TABLE_NAME = 'paths'
    COLUMNS = ['id', 'simulation_id', 'name', 'datetime_created']
    COLUMNS_PRIMARY_KEY = ['id']

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the user has at least the given auth level over this Path."""

        # Get the User id

        try:
            user_id = User.from_google_id(google_id).read().id
        except exceptions.RowNotFoundError:
            return False

        # Check the Authorization

        authorization = Authorization.from_primary_key((user_id, self.simulation_id))

        return authorization.has_at_least(authorization_level)
