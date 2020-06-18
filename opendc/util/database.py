import json
import sys

from datetime import datetime
from pymongo import MongoClient
from bson.json_util import loads, dumps, RELAXED_JSON_OPTIONS, CANONICAL_JSON_OPTIONS
import urllib.parse

#from mysql.connector.pooling import MySQLConnectionPool

# Get keys from config file
with open(sys.argv[1]) as f:
    KEYS = json.load(f)

DATETIME_STRING_FORMAT = '%Y-%m-%dT%H:%M:%S'
CONNECTION_POOL = None


def init_connection_pool(user, password, database, host, port):
    user = urllib.parse.quote_plus(user) #TODO: replace this with environment variable
    password = urllib.parse.quote_plus(password) #TODO: same as above
    database = urllib.parse.quote_plus(database)
    host = urllib.parse.quote_plus(host)

    global client 
    global opendcdb
    global prefabs_collection
    global user_collection
    global topologies_collection

    client = MongoClient('mongodb://%s:%s@%s/default_db?authSource=%s' % (user, password, host, database))
    opendcdb = client.opendc
    prefabs_collection = opendcdb.prefabs
    topologies_collection = opendcdb.topologies
    user_collection = opendcdb.users


    #global CONNECTION_POOL
    #CONNECTION_POOL = MySQLConnectionPool(pool_name="opendcpool", pool_size=5,
                                          #user=user, password=password, database=database, host=host, port=port)


def execute(statement, t):
    """Open a database connection and execute the statement."""

    # Connect to the database
    connection = CONNECTION_POOL.get_connection()
    cursor = connection.cursor()

    # Execute the statement
    cursor.execute(statement, t)

    # Get the id
    cursor.execute('SELECT last_insert_id();')
    row_id = cursor.fetchone()[0]

    # Disconnect from the database
    connection.commit()
    connection.close()

    # Return the id
    return row_id


def fetchone(statement, t=None):
    """Open a database connection and return the first row matched by the SELECT statement."""

    # Connect to the database
    connection = CONNECTION_POOL.get_connection()
    cursor = connection.cursor()

    # Execute the SELECT statement

    if t is not None:
        cursor.execute(statement, t)
    else:
        cursor.execute(statement)

    value = cursor.fetchone()

    # Disconnect from the database and return
    connection.close()
    return value


def fetchall(statement, t=None):
    """Open a database connection and return all rows matched by the SELECT statement."""

    # Connect to the database
    connection = CONNECTION_POOL.get_connection()
    cursor = connection.cursor()

    # Execute the SELECT statement

    if t is not None:
        cursor.execute(statement, t)
    else:
        cursor.execute(statement)

    values = cursor.fetchall()

    # Disconnect from the database and return
    connection.close()
    return values


def fetchone(query, collection):
    """Uses existing mongo connection to return a single (the first) document in a collection matching the given query as a JSON object"""
    # query needs to be in json format, i.e.: {'name': prefab_name}
    #TODO: determine which collection to pull from
    bson = prefabs_collection.find_one(query)
    json_string = dumps(bson) #convert BSON representation to JSON
    json_obj = json.loads(json_string) #load as a JSON object
    #leave the id field in for now, we can use it later
    #json_obj.pop("_id")
    return json_obj



def fetchall(query, collection):
    """Uses existing mongo connection to return all documents matching a given query, as a list of JSON objects"""
    results = []
    cursor = prefabs_collection.find(query)
    for doc in cursor:
        json_string = dumps(doc) #convert BSON representation to JSON
        json_obj = json.loads(json_string) #load as a JSON object
        #leave the id field in for now, we can use it later
        #json_obj.pop("_id")
        results.append(json_obj)
    return results


def datetime_to_string(datetime_to_convert):
    """Return a database-compatible string representation of the given datetime object."""

    return datetime_to_convert.strftime(DATETIME_STRING_FORMAT)


def string_to_datetime(string_to_convert):
    """Return a datetime corresponding to the given string representation."""

    return datetime.strptime(string_to_convert, DATETIME_STRING_FORMAT)
