from opendc.models.project import Project
from opendc.models.model import Model


class Portfolio(Model):
    """Model representing a Portfolio."""

    collection_name = 'portfolios'

    def check_user_access(self, user_id, edit_access):
        """Raises an error if the user with given [user_id] has insufficient access.

        Checks access on the parent project.

        :param user_id: The User ID of the user.
        :param edit_access: True when edit access should be checked, otherwise view access.
        """
        project = Project.from_id(self.obj['projectId'])
        project.check_user_access(user_id, edit_access)
