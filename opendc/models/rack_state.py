from opendc.models.model import Model
from opendc.util import database


class RackState(Model):
    JSON_TO_PYTHON_DICT = {
        'RackState': {
            'rackId': 'rack_id',
            'loadFraction': 'load_fraction',
            'tick': 'tick'
        }
    }

    @classmethod
    def _from_database_row(cls, row):
        """Instantiate a RackState from a database row."""

        return cls(
            rack_id=row[0],
            load_fraction=row[1],
            tick=row[2]
        )

    @classmethod
    def from_experiment_id(cls, experiment_id):
        """Query RackStates by their Experiment id."""

        rack_states = []

        statement = '''
            SELECT racks.id, avg(machine_states.load_fraction), machine_states.tick
            FROM racks
                JOIN machines ON racks.id = machines.rack_id
                JOIN machine_states ON machines.id = machine_states.machine_id
            WHERE machine_states.experiment_id = %s
            GROUP BY machine_states.tick, racks.id
        '''
        results = database.fetchall(statement, (experiment_id,))

        for row in results:
            rack_states.append(cls._from_database_row(row))

        return rack_states

    @classmethod
    def from_experiment_id_and_tick(cls, experiment_id, tick):
        """Query RackStates by their Experiment id."""

        rack_states = []

        statement = '''
            SELECT racks.id, avg(machine_states.load_fraction), machine_states.tick
            FROM racks
                JOIN machines ON racks.id = machines.rack_id
                JOIN machine_states ON machines.id = machine_states.machine_id
            WHERE machine_states.experiment_id = %s
                AND machine_states.tick = %s
            GROUP BY machine_states.tick, racks.id
        '''
        results = database.fetchall(statement, (experiment_id, tick))

        for row in results:
            rack_states.append(cls._from_database_row(row))

        return rack_states

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the User has at least the given auth level over this RackState."""

        if authorization_level in ['EDIT', 'OWN']:
            return False

        return True
