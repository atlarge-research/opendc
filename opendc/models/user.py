from opendc.util.database import DB
from opendc.util.rest import Response


class User:
    def __init__(self, obj_id):
        self.obj_id = obj_id
        self.obj = DB.fetch_one({'_id': obj_id}, 'users')

    def validate(self, request_google_id=None):
        if self.obj is None:
            return Response(404, f'User with ID {self.obj_id} not found.')

        if request_google_id is not None and self.obj['googleId'] != request_google_id:
            return Response(403, f'Forbidden from editing user with ID {self.obj_id}.')

        return None

    def set_property(self, key, value):
        self.obj[key] = value

    def update(self):
        DB.update(self.obj_id, self.obj, 'users')

    def delete(self):
        DB.delete_one({'_id': self.obj_id}, 'users')
