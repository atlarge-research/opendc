from opendc.models.model import Model
from opendc.util.database import DB
from opendc.util.rest import Response


class User(Model):
    collection_name = 'users'

    @classmethod
    def from_email(cls, email):
        return User(DB.fetch_one({'email': email}, User.collection_name))

    @classmethod
    def from_google_id(cls, google_id):
        return User(DB.fetch_one({'googleId': google_id}, User.collection_name))

    def validate(self, request_google_id=None):
        super_validation = super().validate(request_google_id)

        if super_validation is not None:
            return super_validation

        if request_google_id is not None and self.obj['googleId'] != request_google_id:
            return Response(403, f'Forbidden from editing user with ID {self.obj["_id"]}.')

        return None

    def validate_insertion(self):
        existing_user = DB.fetch_one({'googleId': self.obj['googleId']}, self.collection_name)

        if existing_user is not None:
            return Response(409, f'User already exists.')

        return None
