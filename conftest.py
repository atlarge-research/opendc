import pytest

from main import FLASK_CORE_APP


@pytest.fixture
def client(mocker):
    """Returns a Flask API client to interact with."""
    FLASK_CORE_APP.config['TESTING'] = True
    mocker.patch('opendc.util.database.DB')

    with FLASK_CORE_APP.test_client() as client:
        yield client
