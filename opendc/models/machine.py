import copy

from opendc.models.model import Model
from opendc.models.rack import Rack
from opendc.util import database, exceptions


class Machine(Model):
    JSON_TO_PYTHON_DICT = {
        'machine': {
            'id': 'id',
            'rackId': 'rack_id',
            'position': 'position',
            'tags': 'tags',
            'cpuIds': 'cpu_ids',
            'gpuIds': 'gpu_ids',
            'memoryIds': 'memory_ids',
            'storageIds': 'storage_ids',
            'topologyId': 'topology_id'
        }
    }

    PATH = '/v1/tiles/{tileId}/rack/machines'

    TABLE_NAME = 'machines'
    COLUMNS = ['id', 'rack_id', 'position', 'topology_id']
    COLUMNS_PRIMARY_KEY = ['id']

    device_table_to_attribute = {
        'cpus': 'cpu_ids',
        'gpus': 'gpu_ids',
        'memories': 'memory_ids',
        'storages': 'storage_ids'
    }

    def _update_devices(self, before_insert):
        """Update this Machine's devices in the database."""

        for device_table in self.device_table_to_attribute.keys():

            # First, delete current machine-device links

            statement = 'DELETE FROM machine_{} WHERE machine_id = %s'.format(device_table)
            database.execute(statement, (before_insert.id,))

            # Then, add current ones

            for device_id in getattr(before_insert, before_insert.device_table_to_attribute[device_table]):
                statement = 'INSERT INTO machine_{} (machine_id, {}) VALUES (%s, %s)'.format(
                    device_table,
                    before_insert.device_table_to_attribute[device_table][:-1]
                )

                database.execute(statement, (before_insert.id, device_id))

    @classmethod
    def from_tile_id_and_rack_position(cls, tile_id, position):
        """Get a Rack from the ID of the tile its Rack is on, and its position in the Rack."""

        try:
            rack = Rack.from_tile_id(tile_id)
        except:
            return cls(id=-1)

        try:
            statement = 'SELECT id FROM machines WHERE rack_id = %s AND position = %s'
            machine_id = database.fetchone(statement, (rack.id, position))[0]
        except:
            return cls(id=-1)

        return cls.from_primary_key((machine_id,))

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the user has at least the given auth level over this Machine."""

        # Get the Rack

        try:
            rack = Rack.from_primary_key((self.rack_id,))
        except exceptions.RowNotFoundError:
            return False

        # Check the Rack's Authorization

        return rack.google_id_has_at_least(google_id, authorization_level)

    def insert(self):
        """Insert this Machine by also updating its devices."""

        before_insert = copy.deepcopy(self)

        super(Machine, self).insert()

        before_insert.id = self.id
        self._update_devices(before_insert)

    def read(self):
        """Read this Machine by also getting its CPU, GPU, Memory and Storage IDs."""

        super(Machine, self).read()

        for device_table in self.device_table_to_attribute.keys():

            statement = 'SELECT * FROM machine_{} WHERE machine_id = %s'.format(device_table)
            results = database.fetchall(statement, (self.id,))

            device_ids = []

            for row in results:
                device_ids.append(row[2])

            setattr(self, self.device_table_to_attribute[device_table], device_ids)

        setattr(self, 'tags', [])

    def update(self):
        """Update this Machine by also updating its devices."""

        before_update = copy.deepcopy(self)

        super(Machine, self).update()

        before_update.id = self.id
        self._update_devices(before_update)
