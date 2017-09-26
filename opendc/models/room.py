from opendc.models.datacenter import Datacenter
from opendc.models.model import Model
from opendc.util import exceptions


class Room(Model):
    JSON_TO_PYTHON_DICT = {
        'room': {
            'id': 'id',
            'datacenterId': 'datacenter_id',
            'name': 'name',
            'roomType': 'type',
            'topologyId': 'topology_id'
        }
    }

    PATH = '/v1/datacenters/{datacenterId}/rooms'

    TABLE_NAME = 'rooms'
    COLUMNS = ['id', 'name', 'datacenter_id', 'type', 'topology_id']
    COLUMNS_PRIMARY_KEY = ['id']

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the user has at least the given auth level over this Room."""

        # Get the Datacenter

        try:
            datacenter = Datacenter.from_primary_key((self.datacenter_id,))
        except exceptions.RowNotFoundError:
            return False

        # Check the Datacenter's Authorization

        return datacenter.google_id_has_at_least(google_id, authorization_level)
