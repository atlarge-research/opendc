from opendc.models.model import Model


class QueuedExperiment(Model):
    JSON_TO_PYTHON_DICT = {
        'QueuedExperiment': {
            'experimentId': 'experiment_id'
        }
    }

    TABLE_NAME = 'queued_experiments'
    COLUMNS = ['experiment_id']
    COLUMNS_PRIMARY_KEY = ['experiment_id']
