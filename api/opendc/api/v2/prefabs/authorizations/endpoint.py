from datetime import datetime

from opendc.models.prefab import Prefab
from opendc.models.user import User
from opendc.util.database import Database
from opendc.util.rest import Response


def GET(request):
    """Return all prefabs the user is authorized to access"""

    user = User.from_id(request.google_id)

    user.check_exists()

    prefab_collection = Prefab.get_all()
    print(type(prefab_collection))
    print(prefab_collection)

    authorizations = { "authorizations" : []}

    for prefab in prefab_collection:
        if prefab['authorId'] == user.get_id() or prefab['visibility'] == "public":
            authorizations["authorizations"].append(prefab)

    return Response(200, 'Successfully fetched authorizations.', authorizations)

