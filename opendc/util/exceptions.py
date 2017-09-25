class RequestInitializationError(Exception):
    """Raised when a Request cannot successfully be initialized"""


class UnimplementedEndpointError(RequestInitializationError):
    """Raised when a Request path does not point to a module."""


class MissingRequestParameterError(RequestInitializationError):
    """Raised when a Request does not contain one or more required parameters."""


class UnsupportedMethodError(RequestInitializationError):
    """Raised when a Request does not use a supported REST method.
    
    The method must be in all-caps, supported by REST, and implemented by the module.
    """


class AuthorizationTokenError(RequestInitializationError):
    """Raised when an authorization token is not correctly verified."""


class ForeignKeyError(Exception):
    """Raised when a foreign key constraint is not met."""


class RowNotFoundError(Exception):
    """Raised when a database row is not found."""

    def __init__(self, table_name):
        super(RowNotFoundError, self).__init__(
            'Row in `{}` table not found.'.format(table_name)
        )

        self.table_name = table_name


class ParameterError(Exception):
    """Raised when a parameter is either missing or incorrectly typed."""


class IncorrectParameterError(ParameterError):
    """Raised when a parameter is of the wrong type."""

    def __init__(self, parameter_name, parameter_location):
        super(IncorrectParameterError, self).__init__(
            'Incorrectly typed `{}` {} parameter.'.format(
                parameter_name,
                parameter_location
            )
        )

        self.parameter_name = parameter_name
        self.parameter_location = parameter_location


class MissingParameterError(ParameterError):
    """Raised when a parameter is missing."""

    def __init__(self, parameter_name, parameter_location):
        super(MissingParameterError, self).__init__(
            'Missing required `{}` {} parameter.'.format(
                parameter_name,
                parameter_location
            )
        )

        self.parameter_name = parameter_name
        self.parameter_location = parameter_location
