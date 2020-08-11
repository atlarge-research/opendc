FROM mongo:4.2.8
MAINTAINER Jacob Burley <j.burley@vu.nl>

# Import init scripts
ADD mongo-init-opendc-db.sh /docker-entrypoint-initdb.d
ADD mongo-init-prefabs-domain-specific.sh /docker-entrypoint-initdb.d
