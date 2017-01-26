if [ ! -f "$1/opendc.db" ]; then
    sqlite3 "$1/opendc.db" < ./database/schema.sql
	sqlite3 "$1/opendc.db" < ./database/test.sql
fi

if [ -z "$OAUTH_CLIENT_ID" ]
then
	echo "OAuth client id not found. Define environment variable OAUTH_CLIENT_ID"
	exit 1
fi

if [ -z "$OAUTH_CLIENT_SECRET" ]
then
	echo "OAuth client secret not found. Define environment variable OAUTH_CLIENT_SECRET"
	exit 2
fi

if [ -z "$SERVER_URL" ]
then
	echo "URL of server not found. Define environment variable SERVER_URL"
	exit 2
fi

sed -i "s/client-id/$OAUTH_CLIENT_ID/g" ./build/keys.json
sed -i "s/client-secret/$OAUTH_CLIENT_SECRET/g" ./build/keys.json

# ignore non ASCII characters
LC_CTYPE=C
LANG=C

find ./opendc-frontend/build -type f -exec sed -i "s/the-google-oauth-client-id/$OAUTH_CLIENT_ID/g" {} \;
find ./opendc-frontend/build -type f -exec sed -i "s,https://opendc.ewi.tudelft.nl:443,$SERVER_URL,g" {} \;
find ./opendc-frontend/build -type f -exec sed -i "s,https://opendc.ewi.tudelft.nl,$SERVER_URL,g" {} \;
find ./opendc-frontend/build -type f -exec sed -i 's,LOCAL_MODE = (document.location.hostname === "localhost"),LOCAL_MODE = false,g' {} \;