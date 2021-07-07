import os
from functools import wraps

from flask import g, _request_ctx_stack
from jose import jwt
from werkzeug.local import LocalProxy

from opendc.database import Database
from opendc.auth import AuthContext, AsymmetricJwtAlgorithm, get_token, AuthError


def get_db():
    """
    Return the configured database instance for the application.
    """
    _db = getattr(g, 'db', None)
    if _db is None:
        _db = Database.from_credentials(user=os.environ['OPENDC_DB_USERNAME'],
                                        password=os.environ['OPENDC_DB_PASSWORD'],
                                        database=os.environ['OPENDC_DB'],
                                        host=os.environ.get('OPENDC_DB_HOST', 'localhost'))
        g.db = _db
    return _db


db = LocalProxy(get_db)


def get_auth_context():
    """
    Return the configured auth context for the application.
    """
    _auth_context = getattr(g, 'auth_context', None)
    if _auth_context is None:
        _auth_context = AuthContext(
            alg=AsymmetricJwtAlgorithm(jwks_url=f"https://{os.environ['AUTH0_DOMAIN']}/.well-known/jwks.json"),
            issuer=f"https://{os.environ['AUTH0_DOMAIN']}/",
            audience=os.environ['AUTH0_AUDIENCE'])
        g.auth_context = _auth_context
    return _auth_context


auth_context = LocalProxy(get_auth_context)


def requires_auth(f):
    """Decorator to determine if the Access Token is valid.
    """
    @wraps(f)
    def decorated(*args, **kwargs):
        token = get_token()
        payload = auth_context.validate(token)
        _request_ctx_stack.top.current_user = payload
        return f(*args, **kwargs)

    return decorated


current_user = LocalProxy(lambda: getattr(_request_ctx_stack.top, 'current_user', None))


def has_scope(required_scope):
    """Determines if the required scope is present in the Access Token
    Args:
        required_scope (str): The scope required to access the resource
    """
    token = get_token()
    unverified_claims = jwt.get_unverified_claims(token)
    if unverified_claims.get("scope"):
        token_scopes = unverified_claims["scope"].split()
        for token_scope in token_scopes:
            if token_scope == required_scope:
                return True
    return False


def requires_scope(required_scope):
    """Determines if the required scope is present in the Access Token
    Args:
        required_scope (str): The scope required to access the resource
    """
    def decorator(f):
        @wraps(f)
        def decorated(*args, **kwargs):
            if not has_scope(required_scope):
                raise AuthError({"code": "Unauthorized", "description": "You don't have access to this resource"}, 403)
            return f(*args, **kwargs)

        return decorated

    return decorator
