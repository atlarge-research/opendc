from opendc.util import database, exceptions


def _missing_parameter(params_required, params_actual, parent=''):
    """Recursively search for the first missing parameter."""

    for param_name in params_required:

        if param_name not in params_actual:
            return '{}.{}'.format(parent, param_name)

        param_required = params_required.get(param_name)
        param_actual = params_actual.get(param_name)

        if isinstance(param_required, dict):

            param_missing = _missing_parameter(
                param_required,
                param_actual,
                param_name
            )

            if param_missing is not None:
                return '{}.{}'.format(parent, param_missing)

    return None


def _incorrect_parameter(params_required, params_actual, parent=''):
    """Recursively make sure each parameter is of the correct type."""

    for param_name in params_required:

        param_required = params_required.get(param_name)
        param_actual = params_actual.get(param_name)

        if isinstance(param_required, dict):

            param_incorrect = _incorrect_parameter(
                param_required,
                param_actual,
                param_name
            )

            if param_incorrect is not None:
                return '{}.{}'.format(parent, param_incorrect)

        else:

            if param_required == 'datetime':
                try:
                    database.string_to_datetime(param_actual)
                except:
                    return '{}.{}'.format(parent, param_name)

            if param_required == 'int' and not isinstance(param_actual, int):
                return '{}.{}'.format(parent, param_name)

            if param_required == 'string' and not isinstance(param_actual, basestring):
                return '{}.{}'.format(parent, param_name)

            if param_required.startswith('list') and not isinstance(param_actual, list):
                return '{}.{}'.format(parent, param_name)


def _format_parameter(parameter):
    """Format the output of a parameter check."""

    parts = parameter.split('.')
    inner = ['["{}"]'.format(x) for x in parts[2:]]
    return parts[1] + ''.join(inner)


def check(request, **kwargs):
    """Return True if all required parameters are there."""

    for location, params_required in kwargs.iteritems():

        params_actual = getattr(request, 'params_{}'.format(location))

        missing_parameter = _missing_parameter(params_required, params_actual)
        if missing_parameter is not None:
            raise exceptions.MissingParameterError(
                _format_parameter(missing_parameter),
                location
            )

        incorrect_parameter = _incorrect_parameter(params_required, params_actual)
        if incorrect_parameter is not None:
            raise exceptions.IncorrectParameterError(
                _format_parameter(incorrect_parameter),
                location
            )
