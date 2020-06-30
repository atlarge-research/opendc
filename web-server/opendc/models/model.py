from opendc.util.database import DB
from opendc.util.exceptions import ClientError
from opendc.util.rest import Response


class Model:
    """Base class for all models."""

    collection_name = '<specified in subclasses>'

    @classmethod
    def from_id(cls, _id):
        """Fetches the document with given ID from the collection."""
        return cls(DB.fetch_one({'_id': _id}, cls.collection_name))

    @classmethod
    def get_all(cls):
        """Fetches all documents from the collection."""
        return cls(DB.fetch_all({}, cls.collection_name))

    def __init__(self, obj):
        self.obj = obj

    def check_exists(self):
        """Raises an error if the enclosed object does not exist."""
        if self.obj is None:
            raise ClientError(Response(404, 'Not found.'))

    def set_property(self, key, value):
        """Sets the given property on the enclosed object."""
        self.obj[key] = value

    def insert(self):
        """Inserts the enclosed object and updates the internal reference to the newly inserted object."""
        self.obj = DB.insert(self.obj, self.collection_name)

    def update(self):
        """Updates the enclosed object and updates the internal reference to the newly inserted object."""
        self.obj = DB.update(self.obj['_id'], self.obj, self.collection_name)

    def delete(self):
        """Deletes the enclosed object in the database."""
        DB.delete_one({'_id': self.obj['_id']}, self.collection_name)
