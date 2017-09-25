from opendc.models.model import Model
from opendc.models.path import Path
from opendc.util import exceptions


class Section(Model):
    JSON_TO_PYTHON_DICT = {
        'Section': {
            'id': 'id',
            'pathId': 'path_id',
            'datacenterId': 'datacenter_id',
            'startTick': 'start_tick'
        }
    }

    TABLE_NAME = 'sections'
    COLUMNS = ['id', 'path_id', 'datacenter_id', 'start_tick']
    COLUMNS_PRIMARY_KEY = ['id']

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the user has at least the given auth level over this Section."""

        # Get the Path

        try:
            path = Path.from_primary_key((self.path_id,))
        except exceptions.RowNotFoundError:
            return False

        # Check the Path's Authorization

        return path.google_id_has_at_least(google_id, authorization_level)
