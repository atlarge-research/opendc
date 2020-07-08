from datetime import datetime

from opendc.models.experiment import Experiment
from opendc.models.prefab import Prefab
from opendc.models.topology import Topology
from opendc.models.user import User
from opendc.util.database import Database
from opendc.util.rest import Response


def GET(request):
    """Get this Prefab."""

    request.check_required_parameters(path={'prefabId': 'string'})

    prefab = Prefab.from_id(request.params_path['prefabId'])

    prefab.check_exists()
    prefab.check_user_access(request.google_id, False)

    return Response(200, 'Successfully retrieved prefab', prefab.obj)


def PUT(request):
    """Update a prefab's name."""

    request.check_required_parameters(body={'prefab': {'name': 'name'}}, path={'prefabId': 'string'})

    prefab = Prefab.from_id(request.params_path['prefabId'])

    prefab.check_exists()
    prefab.check_user_access(request.google_id, True)

    prefab.set_property('name', request.params_body['prefab']['name'])
    prefab.set_property('datetime_last_edited', Database.datetime_to_string(datetime.now()))
    prefab.update()

    return Response(200, 'Successfully updated prefab.', prefab.obj)


def DELETE(request):
    """Delete this Prefab."""

    request.check_required_parameters(path={'prefabId': 'string'})

    prefab = Prefab.from_id(request.params_path['prefabId'])

    prefab.check_exists()
    prefab.check_user_access(request.google_id, True)

    user = User.from_google_id(request.google_id)
    user.obj['authorizations'] = list(
        filter(lambda x: str(x['prefabId']) != request.params_path['prefabId'], user.obj['authorizations']))
    user.update()

    old_object = prefab.delete()

    return Response(200, 'Successfully deleted prefab.', old_object)
