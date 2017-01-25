from datetime import datetime
import json
import sqlite3
import sys

# Get keys from config file
with open(sys.argv[1]) as file:
    KEYS = json.load(file)

DATETIME_STRING_FORMAT = '%Y-%m-%dT%H:%M:%S'

def execute(statement, t):
    """Open a database connection and execute the statement."""

    # Connect to the database
    connection = sqlite3.connect(KEYS['DATABASE_LOCATION'])
    cursor = connection.cursor()

    # Turn on foreign key checks
    cursor.execute('pragma foreign_keys=ON')
    
    # Execute the statement
    cursor.execute(statement, t)

    # Get the id
    database_id = cursor.execute('SELECT last_insert_rowid()').fetchone()[0]

    # Disconnect from the database
    connection.commit()
    connection.close()

    # Return the id
    return database_id

def fetchone(statement, t=None):
    """Open a database connection and return the first row matched by the SELECT statement."""

    # Connect to the database
    connection = sqlite3.connect(KEYS['DATABASE_LOCATION'])
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
    connection = sqlite3.connect(KEYS['DATABASE_LOCATION'])
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

def datetime_to_string(datetime_to_convert):
    """Return a database-compatible string representation of the given datetime object."""

    return datetime_to_convert.strftime(DATETIME_STRING_FORMAT)

def string_to_datetime(string_to_convert):
    """Return a datetime corresponding to the given string representation."""

    return datetime.strptime(string_to_convert, DATETIME_STRING_FORMAT)
