#!/bin/bash

echo 'Creating opendc user and db'

mongo opendc --host localhost \
        --port 27017 \
        -u $MONGO_INITDB_ROOT_USERNAME \
        -p $MONGO_INITDB_ROOT_PASSWORD \
        --authenticationDatabase admin \
        --eval "db.createUser({user: '$OPENDC_DB_USERNAME', pwd: '$OPENDC_DB_PASSWORD', roles:[{role:'dbOwner', db: '$OPENDC_DB'}]});"
MONGO_ROOT_CMD="mongo $OPENDC_DB --host localhost --port 27017 -u $MONGO_INITDB_ROOT_USERNAME -p $MONGO_INITDB_ROOT_PASSWORD --authenticationDatabase admin"

#echo 'Creating opendc db schema...'
MONGO_CMD="mongo $OPENDC_DB -u $OPENDC_DB_USERNAME -p $OPENDC_DB_PASSWORD --authenticationDatabase $OPENDC_DB"
$MONGO_CMD --eval 'db.createCollection("prefabs");'

$MONGO_CMD --eval 'db.prefabs.insertOne(
    {
        "type": "rack",
        "name": "test_rack3",
        "size": 42,
        "depth": 42,
        "author": "Jacob Burley",
        "visibility": "public",
        "children": [
            {
                "type": "switch",
                "ports": 48,
                "power_draw": 150,
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
                        "dimm_slots": 4,
                        "nics": 1,
                        "pcie_slots": 2,
                        "children": [
                            {
                                "type": "CPU",
                                "coreCount": 4,
                                "SMT": true,
                                "base_clk": 3.5,
                                "boost_clk": 3.9,
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
                                "pcie_gen": "3x16",
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
                        "form_factor": 2.5
                    }
                ]
            }
        ]
    });'

# $MONGO_CMD --eval 'db.createCollection("prefabs", {
# 	validator: {
# 		$jsonSchema: {
# 			bsonType: "object",
# 			required: ["name"],
# 			properties: {
# 				name: {
# 					bsonType: "string",
# 					description: "The name of the environment i.e. Production, or Compute Cluster"
# 				},
# 				datacenters: {
# 					bsonType: "object",
# 					required: ["name, location, length, width, height"],
# 					properties: {
# 						name: {
# 							bsonType: "string",
# 							description: "The name of the datacenter i.e. eu-west-1, or Science Building"
# 						},
# 						location: {
# 							bsonType: "string",
# 							description: "The location of the datacenter i.e. Frankfurt, or De Boelelaan 1105"
# 						},
# 						length: {
# 							bsonType: "double",
# 							description: "The physical length of the datacenter, in centimetres"
# 						},
# 						width: {
# 							bsonType: "double",
# 							description: "The physical width of the datacenter, in centimetres"
# 						},
# 						height: {
# 							bsonType: "double",
# 							description: "The physical height of the datacenter, in centimetres"
# 						}
# 					}
# 				}
# 			}
# 		}
# 	}
# });'