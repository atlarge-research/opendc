#!/bin/bash

echo 'Creating OpenDC user and database'

mongo opendc --host localhost \
        --port 27017 \
        -u "$MONGO_INITDB_ROOT_USERNAME" \
        -p "$MONGO_INITDB_ROOT_PASSWORD" \
        --authenticationDatabase admin \
        --eval "db.createUser({user: '$OPENDC_DB_USERNAME', pwd: '$OPENDC_DB_PASSWORD', roles:[{role:'dbOwner', db: '$OPENDC_DB'}]});"

MONGO_CMD="mongo $OPENDC_DB -u $OPENDC_DB_USERNAME -p $OPENDC_DB_PASSWORD --authenticationDatabase $OPENDC_DB"

echo 'Creating collections'

$MONGO_CMD --eval 'db.createCollection("authorizations");'
$MONGO_CMD --eval 'db.createCollection("projects");'
$MONGO_CMD --eval 'db.createCollection("topologies");'
$MONGO_CMD --eval 'db.createCollection("portfolios");'
$MONGO_CMD --eval 'db.createCollection("scenarios");'
$MONGO_CMD --eval 'db.createCollection("traces");'
$MONGO_CMD --eval 'db.createCollection("prefabs");'

echo 'Loading default traces'

$MONGO_CMD --eval 'db.traces.update(
    {"_id": "bitbrains-small"},
    {
        "$set": {
            "_id": "bitbrains-small",
            "name": "bitbrains-small",
            "type": "VM",
        }
    },
    {"upsert": true}
);'
