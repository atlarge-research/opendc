from opendc.models.model import Model
from opendc.models.user import User
from opendc.util.exceptions import ClientError
from opendc.util.rest import Response


class Trace(Model):
    collection_name = 'traces'
