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

MONGO_COMMAND="mongo $MONGO_DB -h $MONGO_DB_HOST --port $MONGO_DB_PORT -u $MONGO_DB_USERNAME -p $MONGO_DB_PASSWORD --authenticationDatabase $MONGO_DB"

until eval $MONGO_COMMAND --eval 'db.getCollectionNames();' ; do
  echo "MongoDB is unavailable - sleeping"
  sleep 1
done

echo "MongoDB available"
