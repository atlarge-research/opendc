if [ -z "$OPENDC_DB" ]; then
    echo "OPENDC_DB environment variable not specified"
    exit 1
fi

if [ -z "$OPENDC_DB_USERNAME" ]; then
    echo "OPENDC_DB_USERNAME environment variable not specified"
    exit 1
fi

if [ -z "$OPENDC_DB_PASSWORD" ]; then
    echo "OPENDC_DB_PASSWORD environment variable not specified"
    exit 1
fi

MONGO_COMMAND="mongo $OPENDC_DB -u $OPENDC_DB_USERNAME -p $OPENDC_DB_PASSWORD --authenticationDatabase $OPENDC_DB"

until eval $MONGO_COMMAND --eval 'db.getCollectionNames();' ; do
  echo "MongoDB is unavailable - sleeping"
  sleep 1
done

echo "MongoDB available"
