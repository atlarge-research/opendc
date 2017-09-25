from opendc.models.model import Model
from opendc.models.user import User


class Authorization(Model):
    JSON_TO_PYTHON_DICT = {
        'Authorization': {
            'userId': 'user_id',
            'simulationId': 'simulation_id',
            'authorizationLevel': 'authorization_level'
        }
    }

    TABLE_NAME = 'authorizations'
    COLUMNS = ['user_id', 'simulation_id', 'authorization_level']
    COLUMNS_PRIMARY_KEY = ['user_id', 'simulation_id']

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the User has at least the given auth level over this Authorization."""

        authorization = Authorization.from_primary_key(
            (
                User.from_google_id(google_id).id,
                self.simulation_id
            )
        )

        if authorization is None:
            return False

        return authorization.has_at_least(authorization_level)

    def has_at_least(self, required_level):
        """Return True if this Authorization has at least the required level."""

        if not self.exists():
            return False

        authorization_levels = ['VIEW', 'EDIT', 'OWN']

        try:
            index_actual = authorization_levels.index(self.authorization_level)
            index_required = authorization_levels.index(required_level)
        except:
            return False

        if index_actual >= index_required:
            return True
        else:
            return False
