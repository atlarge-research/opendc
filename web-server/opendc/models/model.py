from opendc.util.database import DB
from opendc.util.exceptions import ClientError
from opendc.util.rest import Response


class Model:
    collection_name = '<specified in subclasses>'

    @classmethod
    def from_id(cls, _id):
        return cls(DB.fetch_one({'_id': _id}, Model.collection_name))

    def __init__(self, obj):
        self.obj = obj

    def check_exists(self):
        if self.obj is None:
            raise ClientError(Response(404, 'Not found.'))

    def set_property(self, key, value):
        self.obj[key] = value

    def insert(self):
        self.obj = DB.insert(self.obj, self.collection_name)

    def update(self):
        self.obj = DB.update(self.obj['_id'], self.obj, self.collection_name)

    def delete(self):
        self.obj = DB.delete_one({'_id': self.obj['_id']}, self.collection_name)
