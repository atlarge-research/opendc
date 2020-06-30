import json
import urllib.parse
from datetime import datetime

from bson.json_util import dumps
from pymongo import MongoClient

DATETIME_STRING_FORMAT = '%Y-%m-%dT%H:%M:%S'
CONNECTION_POOL = None


class Database:
    """Object holding functionality for database access."""
    def __init__(self):
        self.opendc_db = None

    def init_database(self, user, password, database, host):
        """Initializes the database connection."""

        user = urllib.parse.quote_plus(user)  # TODO: replace this with environment variable
        password = urllib.parse.quote_plus(password)  # TODO: same as above
        database = urllib.parse.quote_plus(database)
        host = urllib.parse.quote_plus(host)

        client = MongoClient('mongodb://%s:%s@%s/default_db?authSource=%s' % (user, password, host, database))
        self.opendc_db = client.opendc

    def fetch_one(self, query, collection):
        """Uses existing mongo connection to return a single (the first) document in a collection matching the given
        query as a JSON object.

        The query needs to be in json format, i.e.: `{'name': prefab_name}`.
        """
        bson = getattr(self.opendc_db, collection).find_one(query)

        return self.convert_bson_to_json(bson)

    def fetch_all(self, query, collection):
        """Uses existing mongo connection to return all documents matching a given query, as a list of JSON objects.

        The query needs to be in json format, i.e.: `{'name': prefab_name}`.
        """
        results = []
        cursor = getattr(self.opendc_db, collection).find(query)
        for doc in cursor:
            results.append(self.convert_bson_to_json(doc))
        return results

    def insert(self, obj, collection):
        """Updates an existing object."""
        bson = getattr(self.opendc_db, collection).insert(obj)

        return self.convert_bson_to_json(bson)

    def update(self, _id, obj, collection):
        """Updates an existing object."""
        bson = getattr(self.opendc_db, collection).update({'_id': _id}, obj)

        return self.convert_bson_to_json(bson)

    def delete_one(self, query, collection):
        """Deletes one object matching the given query.

        The query needs to be in json format, i.e.: `{'name': prefab_name}`.
        """
        bson = getattr(self.opendc_db, collection).delete_one(query)

        return self.convert_bson_to_json(bson)

    def delete_all(self, query, collection):
        """Deletes all objects matching the given query.

        The query needs to be in json format, i.e.: `{'name': prefab_name}`.
        """
        bson = getattr(self.opendc_db, collection).delete_many(query)

        return self.convert_bson_to_json(bson)

    @staticmethod
    def convert_bson_to_json(bson):
        """Converts a BSON representation to JSON and returns the JSON representation."""
        json_string = dumps(bson)
        return json.loads(json_string)

    @staticmethod
    def datetime_to_string(datetime_to_convert):
        """Return a database-compatible string representation of the given datetime object."""
        return datetime_to_convert.strftime(DATETIME_STRING_FORMAT)

    @staticmethod
    def string_to_datetime(string_to_convert):
        """Return a datetime corresponding to the given string representation."""
        return datetime.strptime(string_to_convert, DATETIME_STRING_FORMAT)


DB = Database()
