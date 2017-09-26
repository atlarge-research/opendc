from opendc.models.model import Model
from opendc.models.object import Object
from opendc.models.room import Room
from opendc.util import exceptions


class Tile(Model):
    JSON_TO_PYTHON_DICT = {
        'tile': {
            'id': 'id',
            'roomId': 'room_id',
            'objectId': 'object_id',
            'objectType': 'object_type',
            'positionX': 'position_x',
            'positionY': 'position_y',
            'topologyId': 'topology_id'
        }
    }

    PATH = '/v1/rooms/{roomId}/tiles'

    TABLE_NAME = 'tiles'
    COLUMNS = ['id', 'position_x', 'position_y', 'room_id', 'object_id', 'topology_id']
    COLUMNS_PRIMARY_KEY = ['id']

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the user has at least the given auth level over this Tile."""

        # Get the Room

        try:
            room = Room.from_primary_key((self.room_id,))
        except exceptions.RowNotFoundError:
            return False

        # Check the Room's Authorization

        return room.google_id_has_at_least(google_id, authorization_level)

    def read(self):
        """Read this Tile by also getting its associated object type."""

        super(Tile, self).read()

        if self.object_id is not None:
            obj = Object.from_primary_key((self.object_id,))
            self.object_type = obj.type
