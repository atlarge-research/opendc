#  Copyright (c) 2021 AtLarge Research
#
#  Permission is hereby granted, free of charge, to any person obtaining a copy
#  of this software and associated documentation files (the "Software"), to deal
#  in the Software without restriction, including without limitation the rights
#  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
#  copies of the Software, and to permit persons to whom the Software is
#  furnished to do so, subject to the following conditions:
#
#  The above copyright notice and this permission notice shall be included in all
#  copies or substantial portions of the Software.
#
#  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
#  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
#  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
#  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
#  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
#  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
#  SOFTWARE.

import json
import time

import urllib3
from flask import request
from jose import jwt, JWTError


def get_token():
    """
    Obtain the Access Token from the Authorization Header
    """
    auth = request.headers.get("Authorization", None)
    if not auth:
        raise AuthError({
            "code": "authorization_header_missing",
            "description": "Authorization header is expected"
        }, 401)

    parts = auth.split()

    if parts[0].lower() != "bearer":
        raise AuthError({
            "code": "invalid_header",
            "description": "Authorization header must start with Bearer"
        }, 401)
    if len(parts) == 1:
        raise AuthError({"code": "invalid_header", "description": "Token not found"}, 401)
    if len(parts) > 2:
        raise AuthError({"code": "invalid_header", "description": "Authorization header must be" " Bearer token"}, 401)

    token = parts[1]
    return token


class AuthError(Exception):
    """
    This error is thrown when the request failed to authorize.
    """
    def __init__(self, error, status_code):
        Exception.__init__(self, error)
        self.error = error
        self.status_code = status_code


class AuthContext:
    """
    This class handles the authorization of requests.
    """
    def __init__(self, alg, issuer, audience):
        self._alg = alg
        self._issuer = issuer
        self._audience = audience

    def validate(self, token):
        """
        Validate the specified JWT token.
        :param token: The authorization token specified by the user.
        :return: The token payload on success, otherwise `AuthError`.
        """
        try:
            header = jwt.get_unverified_header(token)
        except JWTError as e:
            raise AuthError({"code": "invalid_token", "message": str(e)}, 401)

        alg = header.get('alg', None)
        if alg != self._alg.algorithm:
            raise AuthError(
                {
                    "code":
                    "invalid_header",
                    "message":
                    f"Signature algorithm of {alg} is not supported. Expected the ID token "
                    f"to be signed with {self._alg.algorithm}"
                }, 401)

        kid = header.get('kid', None)
        try:
            secret_or_certificate = self._alg.get_key(key_id=kid)
        except TokenValidationError as e:
            raise AuthError({"code": "invalid_header", "message": str(e)}, 401)
        try:
            payload = jwt.decode(token,
                                 key=secret_or_certificate,
                                 algorithms=[self._alg.algorithm],
                                 audience=self._audience,
                                 issuer=self._issuer)
            return payload
        except jwt.ExpiredSignatureError:
            raise AuthError({"code": "token_expired", "message": "Token is expired"}, 401)
        except jwt.JWTClaimsError:
            raise AuthError(
                {
                    "code": "invalid_claims",
                    "message": "Incorrect claims, please check the audience and issuer"
                }, 401)
        except Exception as e:
            print(e)
            raise AuthError({"code": "invalid_header", "message": "Unable to parse authentication token."}, 401)


class SymmetricJwtAlgorithm:
    """Verifier for HMAC signatures, which rely on shared secrets.
    Args:
        shared_secret (str): The shared secret used to decode the token.
        algorithm (str, optional): The expected signing algorithm. Defaults to "HS256".
    """
    def __init__(self, shared_secret, algorithm="HS256"):
        self.algorithm = algorithm
        self._shared_secret = shared_secret

    # pylint: disable=W0613
    def get_key(self, key_id=None):
        """
        Obtain the key for this algorithm.
        :param key_id: The identifier of the key.
        :return: The JWK key.
        """
        return self._shared_secret


class AsymmetricJwtAlgorithm:
    """Verifier for RSA signatures, which rely on public key certificates.
    Args:
        jwks_url (str): The url where the JWK set is located.
        algorithm (str, optional): The expected signing algorithm. Defaults to "RS256".
    """
    def __init__(self, jwks_url, algorithm="RS256"):
        self.algorithm = algorithm
        self._fetcher = JwksFetcher(jwks_url)

    def get_key(self, key_id=None):
        """
        Obtain the key for this algorithm.
        :param key_id: The identifier of the key.
        :return: The JWK key.
        """
        return self._fetcher.get_key(key_id)


class TokenValidationError(Exception):
    """
    Error thrown when the token cannot be validated
    """


class JwksFetcher:
    """Class that fetches and holds a JSON web key set.
    This class makes use of an in-memory cache. For it to work properly, define this instance once and re-use it.
    Args:
        jwks_url (str): The url where the JWK set is located.
        cache_ttl (str, optional): The lifetime of the JWK set cache in seconds. Defaults to 600 seconds.
    """
    CACHE_TTL = 600  # 10 min cache lifetime

    def __init__(self, jwks_url, cache_ttl=CACHE_TTL):
        self._jwks_url = jwks_url
        self._http = urllib3.PoolManager()
        self._cache_value = {}
        self._cache_date = 0
        self._cache_ttl = cache_ttl
        self._cache_is_fresh = False

    def _fetch_jwks(self, force=False):
        """Attempts to obtain the JWK set from the cache, as long as it's still valid.
        When not, it will perform a network request to the jwks_url to obtain a fresh result
        and update the cache value with it.
        Args:
            force (bool, optional): whether to ignore the cache and force a network request or not. Defaults to False.
        """
        has_expired = self._cache_date + self._cache_ttl < time.time()

        if not force and not has_expired:
            # Return from cache
            self._cache_is_fresh = False
            return self._cache_value

        # Invalidate cache and fetch fresh data
        self._cache_value = {}
        response = self._http.request('GET', self._jwks_url)

        if response.status == 200:
            # Update cache
            jwks = json.loads(response.data.decode('utf-8'))
            self._cache_value = self._parse_jwks(jwks)
            self._cache_is_fresh = True
            self._cache_date = time.time()
        return self._cache_value

    @staticmethod
    def _parse_jwks(jwks):
        """Converts a JWK string representation into a binary certificate in PEM format.
        """
        keys = {}

        for key in jwks['keys']:
            keys[key["kid"]] = key
        return keys

    def get_key(self, key_id):
        """Obtains the JWK associated with the given key id.
        Args:
            key_id (str): The id of the key to fetch.
        Returns:
            the JWK associated with the given key id.

        Raises:
            TokenValidationError: when a key with that id cannot be found
        """
        keys = self._fetch_jwks()

        if keys and key_id in keys:
            return keys[key_id]

        if not self._cache_is_fresh:
            keys = self._fetch_jwks(force=True)
            if keys and key_id in keys:
                return keys[key_id]
        raise TokenValidationError(f"RSA Public Key with ID {key_id} was not found.")
