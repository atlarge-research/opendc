from opendc.models.model import Model
from opendc.util import database


class TaskState(Model):
    JSON_TO_PYTHON_DICT = {
        'TaskState': {
            'id': 'id',
            'taskId': 'task_id',
            'experimentId': 'experiment_id',
            'tick': 'tick',
            'flopsLeft': 'flops_left',
            'coresUsed': 'cores_used'
        }
    }

    TABLE_NAME = 'task_states'

    COLUMNS = ['id', 'task_id', 'experiment_id', 'tick', 'flops_left', 'cores_used']
    COLUMNS_PRIMARY_KEY = ['id']

    @classmethod
    def from_experiment_id_and_tick(cls, experiment_id, tick):
        """Query Task States by their Experiment id and tick."""

        task_states = []

        statement = 'SELECT * FROM task_states WHERE experiment_id = %s AND tick = %s'
        results = database.fetchall(statement, (experiment_id, tick))

        for row in results:
            task_states.append(
                cls(
                    id=row[0],
                    task_id=row[1],
                    experiment_id=row[2],
                    tick=row[3],
                    flops_left=row[4]
                )
            )

        return task_states

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the User has at least the given auth level over this TaskState."""

        if authorization_level in ['EDIT', 'OWN']:
            return False

        return True
