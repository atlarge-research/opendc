from opendc.models.model import Model
from opendc.util import database


class RoomState(Model):
    JSON_TO_PYTHON_DICT = {
        'RoomState': {
            'roomId': 'room_id',
            'loadFraction': 'load_fraction',
            'tick': 'tick'
        }
    }

    @classmethod
    def _from_database_row(cls, row):
        """Instantiate a RoomState from a database row."""

        return cls(
            room_id=row[0],
            load_fraction=row[1],
            tick=row[2]
        )

    @classmethod
    def from_experiment_id(cls, experiment_id):
        """Query RoomStates by their Experiment id."""

        room_states = []

        statement = '''
            SELECT rooms.id, avg(machine_states.load_fraction), machine_states.tick
            FROM rooms
                JOIN tiles ON rooms.id = tiles.room_id
                JOIN objects ON tiles.object_id = objects.id
                JOIN racks ON objects.id = racks.id
                JOIN machines ON racks.id = machines.rack_id
                JOIN machine_states ON machines.id = machine_states.machine_id
            WHERE objects.type = "RACK"
                AND machine_states.experiment_id = %s
            GROUP BY machine_states.tick, rooms.id
        '''
        results = database.fetchall(statement, (experiment_id,))

        for row in results:
            room_states.append(cls._from_database_row(row))

        return room_states

    @classmethod
    def from_experiment_id_and_tick(cls, experiment_id, tick):
        """Query RoomStates by their Experiment id."""

        room_states = []

        statement = '''
            SELECT rooms.id, avg(machine_states.load_fraction), machine_states.tick
            FROM rooms
                JOIN tiles ON rooms.id = tiles.room_id
                JOIN objects ON tiles.object_id = objects.id
                JOIN racks ON objects.id = racks.id
                JOIN machines ON racks.id = machines.rack_id
                JOIN machine_states ON machines.id = machine_states.machine_id
            WHERE objects.type = "RACK"
                AND machine_states.experiment_id = %s
                AND machine_states.tick = %s
            GROUP BY rooms.id
        '''
        results = database.fetchall(statement, (experiment_id, tick))

        for row in results:
            room_states.append(cls._from_database_row(row))

        return room_states

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the User has at least the given auth level over this RackState."""

        if authorization_level in ['EDIT', 'OWN']:
            return False

        return True
