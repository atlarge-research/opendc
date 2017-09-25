from opendc.models.model import Model


class Object(Model):
    JSON_TO_PYTHON_DICT = {
        'Object': {
            'id': 'id',
            'type': 'type'
        }
    }

    TABLE_NAME = 'objects'
    COLUMNS = ['id', 'type']
    COLUMNS_PRIMARY_KEY = ['id']

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the user has at least the given auth level over this Tile."""

        return True
