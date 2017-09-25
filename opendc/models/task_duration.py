from opendc.models.model import Model
from opendc.util import database


class TaskDuration(Model):
    JSON_TO_PYTHON_DICT = {
        'TaskDuration': {
            'taskId': 'task_id',
            'duration': 'duration'
        }
    }

    @classmethod
    def _from_database_row(cls, row):
        """Instantiate a RoomState from a database row."""

        return cls(
            task_id=row[0],
            duration=row[1]
        )

    @classmethod
    def from_experiment_id(cls, experiment_id):
        """Query RoomStates by their Experiment id."""

        room_states = []

        statement = '''
            SELECT task_id, MAX(tick) - MIN(tick) as duration FROM task_states
            WHERE experiment_id = %s
            GROUP BY task_id
        '''

        results = database.fetchall(statement, (experiment_id,))

        for row in results:
            room_states.append(cls._from_database_row(row))

        return room_states

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the User has at least the given auth level over this TaskDuration."""

        if authorization_level in ['EDIT', 'OWN']:
            return False

        return True
