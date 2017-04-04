if [ ! -f "$1/opendc.db" ]; then
    sqlite3 "$1/opendc.db" < ./database/schema.sql
	sqlite3 "$1/opendc.db" < ./database/test.sql
fi

# ignore non ASCII characters
LC_CTYPE=C
LANG=C
