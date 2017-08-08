from opendc.models.model import Model

class Task(Model):
    
    JSON_TO_PYTHON_DICT = {
        'Task': {
            'id': 'id',
            'startTick': 'start_tick',
            'totalFlopCount': 'total_flop_count',
            'jobId': 'job_id',
            'taskDependencyId': 'task_dependency_id',
            'parallelizability': 'parallelizability'
        }
    }

    TABLE_NAME = 'tasks'
    COLUMNS = ['id', 'start_tick', 'total_flop_count', 'job_id', 'task_dependency_id', 'parallelizability']
    COLUMNS_PRIMARY_KEY = ['id']

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the user has at least the given auth level over this Task."""

        return authorization_level not in ['EDIT', 'OWN']
