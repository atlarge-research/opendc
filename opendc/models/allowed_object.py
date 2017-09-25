from opendc.models.model import Model


class AllowedObject(Model):
    JSON_TO_PYTHON_DICT = {
        'AllowedObject': {
            'roomType': 'room_type',
            'objectType': 'object_type'
        }
    }

    TABLE_NAME = 'allowed_objects'
    COLUMNS = ['room_type', 'object_type']
    COLUMNS_PRIMARY_KEY = ['room_type', 'object_type']

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the user has at least the given auth level over this AllowedObject."""

        if authorization_level in ['EDIT', 'OWN']:
            return False

        return True

    def to_JSON(self):
        """Return a JSON representation of this AllowedObject."""

        return self.object_type
