"""
Configuration file for all unit tests.
"""
import pytest

from app import app


@pytest.fixture
def client():
    """Returns a Flask API client to interact with."""
    app.config['TESTING'] = True

    with app.test_client() as client:
        yield client
