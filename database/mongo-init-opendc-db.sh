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

$MONGO_CMD --eval 'db.createCollection("users");'
$MONGO_CMD --eval 'db.createCollection("projects");'
$MONGO_CMD --eval 'db.createCollection("topologies");'
$MONGO_CMD --eval 'db.createCollection("portfolios");'
$MONGO_CMD --eval 'db.createCollection("scenarios");'
$MONGO_CMD --eval 'db.createCollection("traces");'
$MONGO_CMD --eval 'db.createCollection("prefabs");'

echo 'Loading test data'

$MONGO_CMD --eval 'db.prefabs.insertOne(
    {
        "type": "rack",
        "name": "testRack3",
        "size": 42,
        "depth": 42,
        "author": "Jacob Burley",
        "visibility": "public",
        "children": [
            {
                "type": "switch",
                "ports": 48,
                "powerDraw": 150,
                "psus": 1,
                "size": 1
            },
            {
                "type": "chassis",
                "size": 4,
                "children": [
                    {
                        "type": "mainboard",
                        "sockets": 1,
                        "dimmSlots": 4,
                        "nics": 1,
                        "pcieSlots": 2,
                        "children": [
                            {
                                "type": "CPU",
                                "coreCount": 4,
                                "SMT": true,
                                "baseClk": 3.5,
                                "boostClk": 3.9,
                                "brand": "Intel",
                                "SKU": "i7-3770K",
                                "socket": "LGA1155",
                                "TDP": 77
                            },
                            {
                                "type": "DDR3",
                                "capacity": 4096,
                                "memfreq": 1333,
                                "ecc": false
                            },
                            {
                                "type": "DDR3",
                                "capacity": 4096,
                                "memfreq": 1333,
                                "ecc": false
                            },
                            {
                                "type": "DDR3",
                                "capacity": 4096,
                                "memfreq": 1333,
                                "ecc": false
                            },
                            {
                                "type": "DDR3",
                                "capacity": 4096,
                                "memfreq": 1333,
                                "ecc": false
                            },
                            {
                                "type": "GPU",
                                "VRAM": 8192,
                                "coreCount": 2304,
                                "brand": "AMD",
                                "technologies": "OpenCL",
                                "pcieGen": "3x16",
                                "tdp": 169,
                                "slots": 2
                            }
                        ]
                    },
                    {
                        "type": "PSU",
                        "wattage": 550,
                        "ac": true
                    },
                    {
                        "type": "disk",
                        "size": 2000,
                        "interface": "SATA",
                        "media": "flash",
                        "formFactor": 2.5
                    }
                ]
            }
        ]
    });'
