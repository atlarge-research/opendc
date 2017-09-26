from opendc.util import database, exceptions


class Model(object):
    # MUST OVERRIDE IN DERIVED CLASS

    JSON_TO_PYTHON_DICT = {
        'Model': {
            'jsonParameterName': 'python_parameter_name'
        }
    }

    PATH = ''
    PATH_PARAMETERS = {}

    TABLE_NAME = ''
    COLUMNS = []
    COLUMNS_PRIMARY_KEY = []

    # INITIALIZATION

    def __init__(self, **kwargs):
        """Initialize a model from its keyword arguments."""

        for name, value in kwargs.items():
            setattr(self, name, value)

    def __repr__(self):
        """Return a string representation of this object."""

        identifiers = []

        for attribute in self.COLUMNS_PRIMARY_KEY:
            identifiers.append('{} = {}'.format(attribute, getattr(self, attribute)))

        return '{} ({})'.format(
            self.TABLE_NAME[:-1].title().replace('_', ''),
            '; '.join(identifiers)
        )

    # JSON CONVERSION METHODS

    @classmethod
    def from_JSON(cls, json_object):
        """Initialize a Model from its JSON object representation."""

        parameters = {}
        parameter_map = cls.JSON_TO_PYTHON_DICT.values()[0]

        for json_name in parameter_map:

            python_name = parameter_map[json_name]

            if json_name in json_object:
                parameters[python_name] = json_object.get(json_name)

            else:
                parameters[python_name] = None

        return cls(**parameters)

    def to_JSON(self):
        """Return a JSON-serializable object representation of this Model."""

        parameters = {}
        parameter_map = self.JSON_TO_PYTHON_DICT.values()[0]

        for json_name in parameter_map:

            python_name = parameter_map[json_name]

            if hasattr(self, python_name):
                parameters[json_name] = getattr(self, python_name)

            else:
                parameters[json_name] = None

        return parameters

    # API CALL GENERATION

    def generate_api_call(self, path_parameters, token):
        """Return a message that can be executed by a Request to recreate this object."""

        return {
            'id': 0,
            'path': self.PATH,
            'method': 'POST',
            'parameters': {
                'body': {
                    self.JSON_TO_PYTHON_DICT.keys()[0]: self.to_JSON()
                },
                'path': path_parameters,
                'query': {}
            },
            'token': token
        }

    # SQL STATEMENT GENERATION METHODS

    @classmethod
    def _generate_insert_columns_string(cls):
        """Generate a SQLite insertion columns string for this Model"""

        return ', '.join(cls.COLUMNS)

    @classmethod
    def _generate_insert_placeholders_string(cls):
        """Generate a SQLite insertion placeholders string for this Model."""

        return ', '.join(['%s'] * len(cls.COLUMNS))

    @classmethod
    def _generate_primary_key_string(cls):
        """Generate the SQLite primary key string for this Model."""

        return ' AND '.join(['{} = %s'.format(x) for x in cls.COLUMNS_PRIMARY_KEY])

    @classmethod
    def _generate_update_columns_string(cls):
        """Generate a SQLite updatable columns string for this Model."""

        return ', '.join(
            ['{} = %s'.format(x) for x in cls.COLUMNS if x not in cls.COLUMNS_PRIMARY_KEY]
        )

    # SQL TUPLE GENERATION METHODS

    def _generate_insert_columns_tuple(self):
        """Generate the tuple of insertion column values for this object."""

        value_list = []

        for column in self.COLUMNS:
            value_list.append(getattr(self, column, None))

        return tuple(value_list)

    def _generate_primary_key_tuple(self):
        """Generate the tuple of primary key values for this object."""

        primary_key_list = []

        for column in self.COLUMNS_PRIMARY_KEY:
            primary_key_list.append(getattr(self, column, None))

        return tuple(primary_key_list)

    def _generate_update_columns_tuple(self):
        """Generate the tuple of updatable column values for this object."""

        value_list = []

        for column in [x for x in self.COLUMNS if x not in self.COLUMNS_PRIMARY_KEY]:
            value_list.append(getattr(self, column, None))

        return tuple(value_list)

    # DATABASE HELPER METHODS

    @classmethod
    def _from_database(cls, statement, values):
        """Initialize a Model by fetching it from the database."""

        parameters = {}
        model_from_database = database.fetchone(statement, values)

        if model_from_database is None:
            return None

        for i in range(len(cls.COLUMNS)):
            parameters[cls.COLUMNS[i]] = model_from_database[i]

        return cls(**parameters)

    # PUBLIC DATABASE INTERACTION METHODS

    @classmethod
    def from_primary_key(cls, primary_key_tuple):
        """Initialize a Model by fetching it by its primary key tuple.
        
        If the primary key does not exist in the database, return a stub.
        """

        query = 'SELECT * FROM {} WHERE {}'.format(
            cls.TABLE_NAME,
            cls._generate_primary_key_string()
        )

        # Return an instantiation of the Model with values from the row if it exists

        model = cls._from_database(query, primary_key_tuple)
        if model is not None:
            return model

        # Return a stub instantiation of the Model with just the primary key if it does not

        parameters = {}
        for i, column in enumerate(cls.COLUMNS_PRIMARY_KEY):
            parameters[column] = primary_key_tuple[i]

        return cls(**parameters)

    @classmethod
    def query(cls, column_name=None, value=None):
        """Return all instances of the Model in the database where column_name = value."""

        if column_name is not None and value is not None:
            statement = 'SELECT * FROM {} WHERE {} = %s'.format(cls.TABLE_NAME, column_name)
            database_models = database.fetchall(statement, (value,))

        else:
            statement = 'SELECT * FROM {}'.format(cls.TABLE_NAME)
            database_models = database.fetchall(statement)

        models = []

        for database_model in database_models:

            parameters = {}
            for i, parameter in enumerate(cls.COLUMNS):
                parameters[parameter] = database_model[i]

            models.append(cls(**parameters))

        return models

    def delete(self):
        """Delete this Model from the database."""

        self.read()

        statement = 'DELETE FROM {} WHERE {}'.format(
            self.TABLE_NAME,
            self._generate_primary_key_string()
        )

        values = self._generate_primary_key_tuple()

        database.execute(statement, values)

    def exists(self, column=None):
        """Return True if this Model exists in the database.
        
        Check the primary key by default, or a column if one is specified.
        """

        query = """
            SELECT EXISTS (
                SELECT 1 FROM {}
                WHERE {}
                LIMIT 1
            )
        """

        if column is None:
            query = query.format(
                self.TABLE_NAME,
                self._generate_primary_key_string()
            )
            values = self._generate_primary_key_tuple()

        else:
            query = query.format(
                self.TABLE_NAME,
                '{} = %s'.format(column)
            )
            values = (getattr(self, column),)

        return database.fetchone(query, values)[0] == 1

    def insert(self):
        """Insert this Model into the database."""

        if hasattr(self, 'id'):
            self.id = None

        self.insert_with_id()

    def insert_with_id(self, is_auto_generated=True):
        """Insert this Model into the database without removing its id."""

        statement = 'INSERT INTO {} ({}) VALUES ({})'.format(
            self.TABLE_NAME,
            self._generate_insert_columns_string(),
            self._generate_insert_placeholders_string()
        )

        values = self._generate_insert_columns_tuple()

        try:
            last_row_id = database.execute(statement, values)
        except Exception as e:
            print e
            raise exceptions.ForeignKeyError(e.message)

        if 'id' in self.COLUMNS_PRIMARY_KEY:
            if is_auto_generated:
                setattr(self, 'id', last_row_id)
            self.read()

    def read(self):
        """Read this Model's non-primary key attributes from the database."""

        if not self.exists():
            raise exceptions.RowNotFoundError(self.TABLE_NAME)

        database_model = self.from_primary_key(self._generate_primary_key_tuple())

        for attribute in self.COLUMNS:
            setattr(self, attribute, getattr(database_model, attribute))

        return self

    def update(self):
        """Update this Model's non-primary key attributes in the database."""

        statement = 'UPDATE {} SET {} WHERE {}'.format(
            self.TABLE_NAME,
            self._generate_update_columns_string(),
            self._generate_primary_key_string()
        )

        values = self._generate_update_columns_tuple() + self._generate_primary_key_tuple()

        try:
            database.execute(statement, values)
        except Exception as e:
            raise exceptions.ForeignKeyError(e.message)
