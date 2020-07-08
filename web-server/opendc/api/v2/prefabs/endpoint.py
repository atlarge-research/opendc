from datetime import datetime

from opendc.models.prefab import Prefab
from opendc.models.user import User
from opendc.util.database import Database
from opendc.util.rest import Response


def POST(request):
    """Create a new prefab, and return that new prefab."""

    request.check_required_parameters(body={'prefab': {'name': 'string'}})

    prefab = Prefab(request.params_body['prefab'])
    prefab.set_property('datetimeCreated', Database.datetime_to_string(datetime.now()))
    prefab.set_property('datetimeLastEdited', Database.datetime_to_string(datetime.now()))

    user = User.from_google_id(request.google_id)
    prefab.set_property('userId', user.get_id)

    prefab.insert()

    return Response(200, 'Successfully created prefab.', prefab.obj)
