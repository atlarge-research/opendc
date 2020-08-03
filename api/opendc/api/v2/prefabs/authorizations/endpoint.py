from opendc.models.prefab import Prefab
from opendc.util.database import DB
from opendc.models.user import User
from opendc.util.rest import Response


def GET(request):
    """Return all prefabs the user is authorized to access"""

    user = User.from_id(request.google_id)

    user.check_exists()


    own_prefabs = DB.fetch_all({'authorId' : user.get_id()}, Prefab.collection_name)
    public_prefabs = DB.fetch_all({'visibility' : 'public'}, Prefab.collection_name)

    authorizations = {"authorizations": []}

    authorizations["authorizations"].append(own_prefabs)
    authorizations["authorizations"].append(public_prefabs)

    return Response(200, 'Successfully fetched authorizations.', authorizations)
