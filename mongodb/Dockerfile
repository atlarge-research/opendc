FROM mongo:4.2.5
MAINTAINER Jacob Burley <j.burley@vu.nl>

# Import init script
ADD mongo-init-opendc-db.sh /docker-entrypoint-initdb.d
ADD mongo-opendc-schema.sh /docker-entrypoint-initdb.d