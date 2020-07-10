from datetime import datetime

from opendc.models.prefab import Prefab
from opendc.util.database import Database
from opendc.util.rest import Response


def GET(request):
    """Get this Prefab."""

    request.check_required_parameters(path={'prefabId': 'string'})

    prefab = Prefab.from_id(request.params_path['prefabId'])
    print(prefab.obj)
    prefab.check_exists()
    print("before cua")
    prefab.check_user_access(request.google_id)
    print("after cua")

    return Response(200, 'Successfully retrieved prefab', prefab.obj)


def PUT(request):
    """Update a prefab's name and/or contents."""

    request.check_required_parameters(body={'prefab': {'name': 'name'}}, path={'prefabId': 'string'})

    prefab = Prefab.from_id(request.params_path['prefabId'])

    prefab.check_exists()
    prefab.check_user_access(request.google_id)

    prefab.set_property('name', request.params_body['prefab']['name'])
    prefab.set_property('rack', request.params_body['prefab']['rack'])
    prefab.set_property('datetime_last_edited', Database.datetime_to_string(datetime.now()))
    prefab.update()

    return Response(200, 'Successfully updated prefab.', prefab.obj)


def DELETE(request):
    """Delete this Prefab."""

    request.check_required_parameters(path={'prefabId': 'string'})

    prefab = Prefab.from_id(request.params_path['prefabId'])

    prefab.check_exists()
    prefab.check_user_access(request.google_id)

    old_object = prefab.delete()

    return Response(200, 'Successfully deleted prefab.', old_object)
