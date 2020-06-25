from opendc.util.database import DB
from opendc.util.rest import Response


class Model:
    collection_name = '<specified in subclasses>'

    @classmethod
    def from_id(cls, _id):
        return cls(DB.fetch_one({'_id': _id}, Model.collection_name))

    def __init__(self, obj):
        self.obj = obj

    def validate(self, request_google_id=None):
        if self.obj is None:
            return Response(404, f'Not found.')

        return None

    def set_property(self, key, value):
        self.obj[key] = value

    def insert(self):
        DB.insert(self.obj, self.collection_name)

    def update(self):
        DB.update(self.obj['_id'], self.obj, self.collection_name)

    def delete(self):
        DB.delete_one({'_id': self.obj['_id']}, self.collection_name)
