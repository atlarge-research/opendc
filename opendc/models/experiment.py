from opendc.models.model import Model
from opendc.models.simulation import Simulation
from opendc.util import database, exceptions

class Experiment(Model):

    JSON_TO_PYTHON_DICT = {
        'Experiment': {
            'id': 'id',
            'simulationId': 'simulation_id',
            'pathId': 'path_id',
            'traceId': 'trace_id',
            'schedulerName': 'scheduler_name',
            'name': 'name'
        }
    }

    TABLE_NAME = 'experiments'
    COLUMNS = ['id', 'simulation_id', 'path_id', 'trace_id', 'scheduler_name', 'name']
    COLUMNS_PRIMARY_KEY = ['id']

    def get_last_simulated_tick(self):
        """Get this Experiment's last simulated tick."""

        statement = '''
            SELECT tick
            FROM task_states
            WHERE experiment_id = ?
            ORDER BY tick DESC
            LIMIT 1
        '''
        result = database.fetchone(statement, (self.id,))

        if result is not None:
            return result[0]

        return -1

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the user has at least the given auth level over this Experiment."""

        # Get the Simulation

        try:
            simulation = Simulation.from_primary_key((self.simulation_id,))
        except exceptions.RowNotFoundError:
            return False

        # Check the Simulation's Authorization

        return simulation.google_id_has_at_least(google_id, authorization_level)
