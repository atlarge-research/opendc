from opendc.models.model import Model
from opendc.models.user import User
from opendc.util.exceptions import ClientError
from opendc.util.rest import Response


class Portfolio(Model):
    """Model representing a Portfolio."""

    collection_name = 'portfolios'

    def check_user_access(self, google_id, edit_access):
        """Raises an error if the user with given [google_id] has insufficient access.

        Checks access on the parent project.

        :param google_id: The Google ID of the user.
        :param edit_access: True when edit access should be checked, otherwise view access.
        """
        user = User.from_google_id(google_id)
        authorizations = list(
            filter(lambda x: str(x['projectId']) == str(self.obj['projectId']), user.obj['authorizations']))
        if len(authorizations) == 0 or (edit_access and authorizations[0]['authorizationLevel'] == 'VIEW'):
            raise ClientError(Response(403, 'Forbidden from retrieving/editing portfolio.'))
