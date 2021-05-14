"""
Configuration file for all unit tests.
"""

from functools import wraps
import pytest
from flask import _request_ctx_stack


def decorator(self, f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        _request_ctx_stack.top.current_user = {'sub': 'test'}
        return f(*args, **kwargs)
    return decorated_function


@pytest.fixture
def client():
    """Returns a Flask API client to interact with."""

    # Disable authorization for test API endpoints
    from opendc.util.auth import AuthManager
    AuthManager.require = decorator

    from app import app

    app.config['TESTING'] = True

    with app.test_client() as client:
        yield client
