import flask
from bson.objectid import ObjectId


class JSONEncoder(flask.json.JSONEncoder):
    """
    A customized JSON encoder to handle unsupported types.
    """
    def default(self, o):
        if isinstance(o, ObjectId):
            return str(o)
        return flask.json.JSONEncoder.default(self, o)
