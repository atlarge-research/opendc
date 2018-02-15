FROM mariadb:10.1
MAINTAINER Fabian Mastenbroek <f.s.mastenbroek@student.tudelft.nl>

# Import schema into database
ADD schema.sql /docker-entrypoint-initdb.d

# Add test data into database
ADD test.sql /docker-entrypoint-initdb.d
