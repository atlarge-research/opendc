from opendc.models.model import Model
from opendc.models.object import Object
from opendc.models.tile import Tile


class Rack(Model):
    JSON_TO_PYTHON_DICT = {
        'rack': {
            'id': 'id',
            'name': 'name',
            'capacity': 'capacity',
            'powerCapacityW': 'power_capacity_w',
            'topologyId': 'topology_id'
        }
    }

    PATH = '/v1/tiles/{tileId}/rack'

    TABLE_NAME = 'racks'
    COLUMNS = ['id', 'name', 'capacity', 'power_capacity_w', 'topology_id']
    COLUMNS_PRIMARY_KEY = ['id']

    @classmethod
    def from_tile_id(cls, tile_id):
        """Get a Rack from the ID of the Tile it's on."""

        tile = Tile.from_primary_key((tile_id,))

        if not tile.exists():
            return Rack(id=-1)

        return cls.from_primary_key((tile.object_id,))

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the user has at least the given auth level over this Rack."""

        # Get the Tile

        try:
            tile = Tile.query('object_id', self.id)[0]
        except:
            return False

        # Check the Tile's Authorization

        return tile.google_id_has_at_least(google_id, authorization_level)

    def insert(self):
        """Insert a Rack by first inserting an object."""

        obj = Object(type='RACK')
        obj.insert()

        self.id = obj.id
        self.insert_with_id(is_auto_generated=False)

    def delete(self):
        """Delete a Rack by deleting its associated object."""

        obj = Object.from_primary_key((self.id,))
        obj.delete()
