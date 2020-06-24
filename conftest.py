import pytest

from main import FLASK_CORE_APP


@pytest.fixture
def client():
    """Returns a Flask API client to interact with."""
    FLASK_CORE_APP.config['TESTING'] = True

    with FLASK_CORE_APP.test_client() as client:
        yield client
