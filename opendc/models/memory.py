from opendc.models.model import Model


class Memory(Model):
    JSON_TO_PYTHON_DICT = {
        'Memory': {
            'id': 'id',
            'manufacturer': 'manufacturer',
            'family': 'family',
            'generation': 'generation',
            'model': 'model',
            'speedMbPerS': 'speed_mb_per_s',
            'sizeMb': 'size_mb',
            'energyConsumptionW': 'energy_consumption_w',
            'failureModelId': 'failure_model_id'
        }
    }

    TABLE_NAME = 'memories'

    COLUMNS = [
        'id',
        'manufacturer',
        'family',
        'generation',
        'model',
        'speed_mb_per_s',
        'size_mb',
        'energy_consumption_w',
        'failure_model_id'
    ]

    COLUMNS_PRIMARY_KEY = ['id']

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the User has at least the given auth level over this Memory."""

        if authorization_level in ['EDIT', 'OWN']:
            return False

        return True
