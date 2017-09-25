from opendc.models.authorization import Authorization
from opendc.models.model import Model
from opendc.models.user import User
from opendc.util import exceptions


class Simulation(Model):
    JSON_TO_PYTHON_DICT = {
        'Simulation': {
            'id': 'id',
            'name': 'name',
            'datetimeCreated': 'datetime_created',
            'datetimeLastEdited': 'datetime_last_edited'
        }
    }

    TABLE_NAME = 'simulations'
    COLUMNS = ['id', 'datetime_created', 'datetime_last_edited', 'name']
    COLUMNS_PRIMARY_KEY = ['id']

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the user has at least the given auth level over this Simulation."""

        # Get the User id

        try:
            user_id = User.from_google_id(google_id).read().id
        except exceptions.RowNotFoundError:
            return False

        # Get the Simulation id

        simulation_id = self.id

        # Check the Authorization

        authorization = Authorization.from_primary_key((user_id, simulation_id))

        return authorization.has_at_least(authorization_level)
