#!/bin/bash

echo 'Creating opendc db schema...'

$MONGO_CMD = "mongo opendc --host localhost --port 27017 -u $OPENDC_DB_USERNAME -p $OPENDC_DB_PASSWORD --authenticationDatabase opendc"

eval $MONGO_COMMAND --eval "db.createCollection('environments'); db.createCollection('rooms'); db.createCollection('datacenters');"