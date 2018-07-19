from opendc.models.model import Model
from opendc.util import database


class MachineState(Model):
    JSON_TO_PYTHON_DICT = {
        'MachineState': {
            'machineId': 'machine_id',
            'temperatureC': 'temperature_c',
            'inUseMemoryMb': 'in_use_memory_mb',
            'loadFraction': 'load_fraction',
            'tick': 'tick'
        }
    }

    TABLE_NAME = 'machine_states'
    COLUMNS = ['id', 'machine_id', 'experiment_id', 'tick', 'temperature_c', 'in_use_memory_mb',
               'load_fraction']

    COLUMNS_PRIMARY_KEY = ['id']

    @classmethod
    def _from_database_row(cls, row):
        """Instantiate a MachineState from a database row (including tick from the TaskState)."""

        return cls(
            machine_id=row[1],
            temperature_c=row[4],
            in_use_memory_mb=row[5],
            load_fraction=row[6],
            tick=row[3]
        )

    @classmethod
    def from_experiment_id(cls, experiment_id):
        """Query MachineStates by their Experiment id."""

        machine_states = []

        statement = 'SELECT * FROM machine_states WHERE experiment_id = %s'
        results = database.fetchall(statement, (experiment_id,))

        for row in results:
            machine_states.append(cls._from_database_row(row))

        return machine_states

    @classmethod
    def from_experiment_id_and_tick(cls, experiment_id, tick):
        """Query MachineStates by their Experiment id and tick."""

        machine_states = []

        statement = 'SELECT * FROM machine_states WHERE experiment_id = %s AND machine_states.tick = %s'
        results = database.fetchall(statement, (experiment_id, tick))

        for row in results:
            machine_states.append(cls._from_database_row(row))

        return machine_states

    def read(self):
        """Read this MachineState by also getting its tick."""

        super(MachineState, self).read()

        statement = 'SELECT tick FROM task_states WHERE id = %s'
        result = database.fetchone(statement, (self.task_state_id,))

        self.tick = result[0]

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the User has at least the given auth level over this MachineState."""

        if authorization_level in ['EDIT', 'OWN']:
            return False

        return True
