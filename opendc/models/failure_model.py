from opendc.models.model import Model


class FailureModel(Model):
    JSON_TO_PYTHON_DICT = {
        'FailureModel': {
            'id': 'id',
            'name': 'name',
            'rate': 'rate'
        }
    }

    TABLE_NAME = 'failure_models'
    COLUMNS = ['id', 'name', 'rate']
    COLUMNS_PRIMARY_KEY = ['id']

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the user has at least the given auth level over this FailureModel."""

        if authorization_level in ['EDIT', 'OWN']:
            return False

        return True
