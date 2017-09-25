from opendc.models.model import Model


class RoomType(Model):
    JSON_TO_PYTHON_DICT = {
        'RoomType': {
            'name': 'name'
        }
    }

    TABLE_NAME = 'room_types'
    COLUMNS = ['name']
    COLUMNS_PRIMARY_KEY = ['name']

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the user has at least the given auth level over this RoomType."""

        if authorization_level in ['EDIT', 'OWN']:
            return False

        return True
