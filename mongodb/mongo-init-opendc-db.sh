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
$MONGO_CMD --eval 'db.createCollection("environments", {
	validator: {
		$jsonSchema: {
			bsonType: "object",
			required: ["name"],
			properties: {
				name: {
					bsonType: "string",
					description: "The name of the environment i.e. Production, or Compute Cluster"
				},
				datacenters: {
					bsonType: "object",
					required: ["name, location, length, width, height"],
					properties: {
						name: {
							bsonType: "string",
							description: "The name of the datacenter i.e. eu-west-1, or Science Building"
						},
						location: {
							bsonType: "string",
							description: "The location of the datacenter i.e. Frankfurt, or De Boelelaan 1105"
						},
						length: {
							bsonType: "double",
							description: "The physical length of the datacenter, in centimetres"
						},
						width: {
							bsonType: "double",
							description: "The physical width of the datacenter, in centimetres"
						},
						height: {
							bsonType: "double",
							description: "The physical height of the datacenter, in centimetres"
						}
					}
				}
			}
		}
	}
});'