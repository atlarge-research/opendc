from opendc.util import exceptions
from opendc.util.database import Database


def _missing_parameter(params_required, params_actual, parent=''):
    """Recursively search for the first missing parameter."""

    for param_name in params_required:

        if param_name not in params_actual:
            return '{}.{}'.format(parent, param_name)

        param_required = params_required.get(param_name)
        param_actual = params_actual.get(param_name)

        if isinstance(param_required, dict):

            param_missing = _missing_parameter(param_required, param_actual, param_name)

            if param_missing is not None:
                return '{}.{}'.format(parent, param_missing)

    return None


def _incorrect_parameter(params_required, params_actual, parent=''):
    """Recursively make sure each parameter is of the correct type."""

    for param_name in params_required:

        param_required = params_required.get(param_name)
        param_actual = params_actual.get(param_name)

        if isinstance(param_required, dict):

            param_incorrect = _incorrect_parameter(param_required, param_actual, param_name)

            if param_incorrect is not None:
                return '{}.{}'.format(parent, param_incorrect)

        else:

            if param_required == 'datetime':
                try:
                    Database.string_to_datetime(param_actual)
                except:
                    return '{}.{}'.format(parent, param_name)

            type_pairs = [
                ('int', (int,)),
                ('float', (float, int)),
                ('bool', (bool,)),
                ('string', (str, int)),
                ('list', (list,)),
            ]

            for str_type, actual_types in type_pairs:
                if param_required == str_type and all(not isinstance(param_actual, t)
                                                      for t in actual_types):
                    return '{}.{}'.format(parent, param_name)

    return None


def _format_parameter(parameter):
    """Format the output of a parameter check."""

    parts = parameter.split('.')
    inner = ['["{}"]'.format(x) for x in parts[2:]]
    return parts[1] + ''.join(inner)


def check(request, **kwargs):
    """Check if all required parameters are there."""

    for location, params_required in kwargs.items():
        params_actual = getattr(request, 'params_{}'.format(location))

        missing_parameter = _missing_parameter(params_required, params_actual)
        if missing_parameter is not None:
            raise exceptions.MissingParameterError(_format_parameter(missing_parameter), location)

        incorrect_parameter = _incorrect_parameter(params_required, params_actual)
        if incorrect_parameter is not None:
            raise exceptions.IncorrectParameterError(_format_parameter(incorrect_parameter), location)
