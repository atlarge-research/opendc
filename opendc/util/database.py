import json
import urllib.parse
from datetime import datetime

from bson.json_util import dumps
from pymongo import MongoClient

DATETIME_STRING_FORMAT = '%Y-%m-%dT%H:%M:%S'
CONNECTION_POOL = None


def init_connection_pool(user, password, database, host, port):
    user = urllib.parse.quote_plus(user)  # TODO: replace this with environment variable
    password = urllib.parse.quote_plus(password)  # TODO: same as above
    database = urllib.parse.quote_plus(database)
    host = urllib.parse.quote_plus(host)

    global opendcdb

    client = MongoClient('mongodb://%s:%s@%s/default_db?authSource=%s' % (user, password, host, database))
    opendcdb = client.opendc


def fetch_one(query, collection):
    """Uses existing mongo connection to return a single (the first) document in a collection matching the given
    query as a JSON object.

    The query needs to be in json format, i.e.: `{'name': prefab_name}`.
    """
    bson = getattr(opendcdb, collection).find_one(query)

    return convert_bson_to_json(bson)


def fetch_all(query, collection):
    """Uses existing mongo connection to return all documents matching a given query, as a list of JSON objects.

    The query needs to be in json format, i.e.: `{'name': prefab_name}`.
    """
    results = []
    cursor = getattr(opendcdb, collection).find(query)
    for doc in cursor:
        results.append(convert_bson_to_json(doc))
    return results


def insert(obj, collection):
    """Updates an existing object."""
    bson = getattr(opendcdb, collection).insert(obj)

    return convert_bson_to_json(bson)


def update(_id, obj, collection):
    """Updates an existing object."""
    bson = getattr(opendcdb, collection).update({'_id': _id}, obj)

    return convert_bson_to_json(bson)


def convert_bson_to_json(bson):
    # Convert BSON representation to JSON
    json_string = dumps(bson)
    # Load as a JSON object
    return json.loads(json_string)


def datetime_to_string(datetime_to_convert):
    """Return a database-compatible string representation of the given datetime object."""

    return datetime_to_convert.strftime(DATETIME_STRING_FORMAT)


def string_to_datetime(string_to_convert):
    """Return a datetime corresponding to the given string representation."""

    return datetime.strptime(string_to_convert, DATETIME_STRING_FORMAT)
