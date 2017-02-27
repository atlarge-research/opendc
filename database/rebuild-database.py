import os
import sqlite3
import sys

sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)

try:
    BASE_DIR = directory_name=sys.argv[1]
except:
    BASE_DIR = os.path.dirname(os.path.abspath(__file__))
db_location = os.path.join(BASE_DIR, 'opendc.db')

print "Removing old database..."
os.remove(db_location)

print "Connecting to new database..."
conn = sqlite3.connect(db_location)
c = conn.cursor()

print "Importing schema..."
with open('schema.sql') as schema:
    c.executescript(schema.read())

print "Importing test data..."
with open('test.sql') as test:
    c.executescript(test.read())

conn.commit()
conn.close()

print "Done."
