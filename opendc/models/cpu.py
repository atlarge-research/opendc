from opendc.models.model import Model


class CPU(Model):
    JSON_TO_PYTHON_DICT = {
        'CPU': {
            'id': 'id',
            'manufacturer': 'manufacturer',
            'family': 'family',
            'generation': 'generation',
            'model': 'model',
            'clockRateMhz': 'clock_rate_mhz',
            'numberOfCores': 'number_of_cores',
            'energyConsumptionW': 'energy_consumption_w',
            'failureModelId': 'failure_model_id'
        }
    }

    TABLE_NAME = 'cpus'

    COLUMNS = [
        'id',
        'manufacturer',
        'family',
        'generation',
        'model',
        'clock_rate_mhz',
        'number_of_cores',
        'energy_consumption_w',
        'failure_model_id'
    ]

    COLUMNS_PRIMARY_KEY = ['id']

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the User has at least the given auth level over this CPU."""

        if authorization_level in ['EDIT', 'OWN']:
            return False

        return True
