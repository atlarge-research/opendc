from opendc.models.model import Model


class Trace(Model):
    JSON_TO_PYTHON_DICT = {
        'Trace': {
            'id': 'id',
            'name': 'name'
        }
    }

    TABLE_NAME = 'traces'
    COLUMNS = ['id', 'name']
    COLUMNS_PRIMARY_KEY = ['id']

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the user has at least the given auth level over this Trace."""

        return authorization_level not in ['EDIT', 'OWN']
