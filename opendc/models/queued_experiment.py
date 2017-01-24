from opendc.models.model import Model
from opendc.models.experiment import Experiment
from opendc.util import database, exceptions

class QueuedExperiment(Model):

    JSON_TO_PYTHON_DICT = {
        'QueuedExperiment': {
            'experimentId': 'experiment_id'
        }
    }

    TABLE_NAME = 'queued_experiments'
    COLUMNS = ['experiment_id']
    COLUMNS_PRIMARY_KEY = ['experiment_id']
