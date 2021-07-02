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

import urllib.parse

from pymongo import MongoClient

DATETIME_STRING_FORMAT = '%Y-%m-%dT%H:%M:%S'
CONNECTION_POOL = None


class Database:
    """Object holding functionality for database access."""
    def __init__(self, db=None):
        """Initializes the database connection."""
        self.opendc_db = db

    @classmethod
    def from_credentials(cls, user, password, database, host):
        """
        Construct a database instance from the specified credentials.
        :param user: The username to connect with.
        :param password: The password to connect with.
        :param database: The database name to connect to.
        :param host: The host to connect to.
        :return: The database instance.
        """
        user = urllib.parse.quote_plus(user)
        password = urllib.parse.quote_plus(password)
        database = urllib.parse.quote_plus(database)
        host = urllib.parse.quote_plus(host)

        client = MongoClient('mongodb://%s:%s@%s/default_db?authSource=%s' % (user, password, host, database))
        return cls(client.opendc)

    def fetch_one(self, query, collection):
        """Uses existing mongo connection to return a single (the first) document in a collection matching the given
        query as a JSON object.

        The query needs to be in json format, i.e.: `{'name': prefab_name}`.
        """
        return getattr(self.opendc_db, collection).find_one(query)

    def fetch_all(self, query, collection):
        """Uses existing mongo connection to return all documents matching a given query, as a list of JSON objects.

        The query needs to be in json format, i.e.: `{'name': prefab_name}`.
        """
        cursor = getattr(self.opendc_db, collection).find(query)
        return list(cursor)

    def insert(self, obj, collection):
        """Updates an existing object."""
        bson = getattr(self.opendc_db, collection).insert(obj)

        return bson

    def update(self, _id, obj, collection):
        """Updates an existing object."""
        return getattr(self.opendc_db, collection).update({'_id': _id}, obj)

    def delete_one(self, query, collection):
        """Deletes one object matching the given query.

        The query needs to be in json format, i.e.: `{'name': prefab_name}`.
        """
        getattr(self.opendc_db, collection).delete_one(query)

    def delete_all(self, query, collection):
        """Deletes all objects matching the given query.

        The query needs to be in json format, i.e.: `{'name': prefab_name}`.
        """
        getattr(self.opendc_db, collection).delete_many(query)
