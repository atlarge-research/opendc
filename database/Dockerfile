FROM mongo:5.0
MAINTAINER OpenDC Maintainers <opendc@atlarge-research.com>

# Import init scripts
ADD mongo-init-opendc-db.sh /docker-entrypoint-initdb.d
ADD mongo-init-prefabs-domain-specific.sh /docker-entrypoint-initdb.d
