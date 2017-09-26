from opendc.models.model import Model
from opendc.models.section import Section


class Datacenter(Model):
    JSON_TO_PYTHON_DICT = {
        'datacenter': {
            'id': 'id',
            'starred': 'starred',
            'simulationId': 'simulation_id'
        }
    }

    PATH = '/v1/simulations/{simulationId}/datacenters'

    TABLE_NAME = 'datacenters'
    COLUMNS = ['id', 'simulation_id', 'starred']
    COLUMNS_PRIMARY_KEY = ['id']

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the user has at least the given auth level over this Datacenter."""

        # Get a Section that contains this Datacenter. It doesn't matter which one, since all Sections that have this
        # Datacenter belong to the same Simulation, so the User's Authorization is the same for each one.

        try:
            section = Section.query('datacenter_id', self.id)[0]
        except:
            return False

        # Check the Section's Authorization

        return section.google_id_has_at_least(google_id, authorization_level)
