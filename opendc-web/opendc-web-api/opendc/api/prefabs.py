#  Copyright (c) 2021 AtLarge Research
#
#  Permission is hereby granted, free of charge, to any person obtaining a copy
#  of this software and associated documentation files (the "Software"), to deal
#  in the Software without restriction, including without limitation the rights
#  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
#  copies of the Software, and to permit persons to whom the Software is
#  furnished to do so, subject to the following conditions:
#
#  The above copyright notice and this permission notice shall be included in all
#  copies or substantial portions of the Software.
#
#  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
#  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
#  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
#  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
#  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
#  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
#  SOFTWARE.

from datetime import datetime
from flask import request
from flask_restful import Resource
from marshmallow import Schema, fields

from opendc.models.prefab import Prefab as PrefabModel, PrefabSchema
from opendc.exts import current_user, requires_auth, db


class PrefabList(Resource):
    """
    Resource for the list of prefabs available to the user.
    """
    method_decorators = [requires_auth]

    def get(self):
        """
        Get the available prefabs for a user.
        """
        user_id = current_user['sub']

        own_prefabs = db.fetch_all({'authorId': user_id}, PrefabModel.collection_name)
        public_prefabs = db.fetch_all({'visibility': 'public'}, PrefabModel.collection_name)

        authorizations = {"authorizations": []}
        authorizations["authorizations"].append(own_prefabs)
        authorizations["authorizations"].append(public_prefabs)
        return {'data': authorizations}

    def post(self):
        """
        Create a new prefab.
        """
        schema = PrefabList.PostSchema()
        result = schema.load(request.json)

        prefab = PrefabModel(result['prefab'])
        prefab.set_property('datetimeCreated', datetime.now())
        prefab.set_property('datetimeLastEdited', datetime.now())

        user_id = current_user['sub']
        prefab.set_property('authorId', user_id)

        prefab.insert()
        data = PrefabSchema().dump(prefab.obj)
        return {'data': data}

    class PostSchema(Schema):
        """
        Schema for the POST operation on the prefab list.
        """
        prefab = fields.Nested(PrefabSchema, required=True)


class Prefab(Resource):
    """
    Resource representing a single prefab.
    """
    method_decorators = [requires_auth]

    def get(self, prefab_id):
        """Get this Prefab."""
        prefab = PrefabModel.from_id(prefab_id)
        prefab.check_exists()
        prefab.check_user_access(current_user['sub'])
        data = PrefabSchema().dump(prefab.obj)
        return {'data': data}

    def put(self, prefab_id):
        """Update a prefab's name and/or contents."""

        schema = Prefab.PutSchema()
        result = schema.load(request.json)

        prefab = PrefabModel.from_id(prefab_id)
        prefab.check_exists()
        prefab.check_user_access(current_user['sub'])

        prefab.set_property('name', result['prefab']['name'])
        prefab.set_property('rack', result['prefab']['rack'])
        prefab.set_property('datetimeLastEdited', datetime.now())
        prefab.update()

        data = PrefabSchema().dump(prefab.obj)
        return {'data': data}

    def delete(self, prefab_id):
        """Delete this Prefab."""
        prefab = PrefabModel.from_id(prefab_id)

        prefab.check_exists()
        prefab.check_user_access(current_user['sub'])

        old_object = prefab.delete()

        data = PrefabSchema().dump(old_object)
        return {'data': data}

    class PutSchema(Schema):
        """
        Schema for the PUT operation on a prefab.
        """
        prefab = fields.Nested(PrefabSchema, required=True)
