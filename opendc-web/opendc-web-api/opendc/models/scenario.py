from opendc.models.model import Model
from opendc.models.portfolio import Portfolio


class Scenario(Model):
    """Model representing a Scenario."""

    collection_name = 'scenarios'

    def check_user_access(self, user_id, edit_access):
        """Raises an error if the user with given [user_id] has insufficient access.

        Checks access on the parent project.

        :param user_id: The User ID of the user.
        :param edit_access: True when edit access should be checked, otherwise view access.
        """
        portfolio = Portfolio.from_id(self.obj['portfolioId'])
        print(portfolio.obj)
        portfolio.check_user_access(user_id, edit_access)
