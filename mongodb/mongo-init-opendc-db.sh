#!/bin/bash

echo 'Creating opendc user and db'

mongo opendc \
        --host localhost \
        --port 27017 \
        -u $MONGO_INITDB_ROOT_USERNAME \
        -p $MONGO_INITDB_ROOT_PASSWORD \
        --authenticationDatabase admin \
        --eval "db.createUser({user: 'opendc', pwd: 'opendcpassword', roles:[{role:'dbOwner', db: 'opendc'}]});"
$MONGO_CMD = "mongo opendc --host localhost --port 27017 -u $MONGO_INITDB_ROOT_USERNAME -p $MONGO_INITDB_ROOT_PASSWORD --authenticationDatabase admin"