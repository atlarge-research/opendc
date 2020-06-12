if [ -z "$MONGO_DB" ]; then
    echo "MONGO_DB environment variable not specified"
    exit 1
fi

if [ -z "$MONGO_DB_USER" ]; then
    echo "MONGO_DB_USER environment variable not specified"
    exit 1
fi

if [ -z "$MONGO_DB_PASSWORD" ]; then
    echo "MONGO_DB_PASSWORD environment variable not specified"
    exit 1
fi

#MYSQL_COMMAND="mysql -h mariadb -u $MYSQL_USER --password=$MYSQL_PASSWORD"

MONGO_COMMAND="mongo $MONGO_DB -h $MONGO_DB_HOST --port $MONGO_DB_PORT -u $MONGO_DB_USERNAME -p $MONGO_DB_PASSWORD --authenticationDatabase $MONGO_DB"

until eval $MONGO_COMMAND --eval 'db.getCollectionNames();' ; do
  echo "MongoDB is unavailable - sleeping"
  sleep 1
done

echo "MongoDB available"

#NUM_TABLES=$(eval "$MYSQL_COMMAND -B --disable-column-names -e \"SELECT count(*) FROM information_schema.tables WHERE table_schema='$MYSQL_DATABASE';\"")

# Check if database is empty
#if [ "$NUM_TABLES" -eq 0 ]; then
#	eval $MYSQL_COMMAND "$MYSQL_DATABASE" < ./database/schema.sql
#	eval $MYSQL_COMMAND "$MYSQL_DATABASE" < ./database/test.sql
#fi

# Writing databse config values to keys.json
cat keys.json | python -c "import os, sys, json; ks = json.load(sys.stdin); \
    ks['MONGODB_HOST'] = os.environ['MONGO_DB_HOST']; \
    ks['MONGODB_PORT'] = os.environ['MONGO_DB_PORT']; \
    ks['MONGODB_DATABASE'] = os.environ['MONGO_DB']; \
    ks['MYSQL_USER'] = os.environ['MONGO_DB_USER']; \
    ks['MYSQL_PASSWORD'] = os.environ['MONGO_DB_PASSWORD']; \
    print json.dumps(ks, indent=4)" > new_keys.json
mv new_keys.json keys.json
