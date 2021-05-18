"""
Configuration file for all unit tests.
"""

from functools import wraps
import pytest
from flask import _request_ctx_stack, g
from opendc.database import Database


def decorator(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        _request_ctx_stack.top.current_user = {'sub': 'test'}
        return f(*args, **kwargs)
    return decorated_function


@pytest.fixture
def client():
    """Returns a Flask API client to interact with."""

    # Disable authorization for test API endpoints
    from opendc import exts
    exts.requires_auth = decorator

    from app import create_app

    app = create_app(testing=True)

    with app.app_context():
        g.db = Database()
        with app.test_client() as client:
            yield client
