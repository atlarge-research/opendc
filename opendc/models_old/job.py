from opendc.models_old.model import Model


class Job(Model):
    JSON_TO_PYTHON_DICT = {'Job': {'id': 'id', 'name': 'name'}}

    COLLECTION_NAME = 'jobs'
    COLUMNS = ['id', 'name']
    COLUMNS_PRIMARY_KEY = ['id']

    def google_id_has_at_least(self, google_id, authorization_level):
        """Return True if the user has at least the given auth level over this Job."""

        return authorization_level not in ['EDIT', 'OWN']
