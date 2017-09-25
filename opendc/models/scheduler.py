from opendc.models.model import Model


class Scheduler(Model):
    JSON_TO_PYTHON_DICT = {
        'Scheduler': {
            'name': 'name'
        }
    }

    TABLE_NAME = 'schedulers'
    COLUMNS = ['name']
    COLUMNS_PRIMARY_KEY = ['name']

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the user has at least the given auth level over this Scheduler."""

        return authorization_level not in ['EDIT', 'OWN']
