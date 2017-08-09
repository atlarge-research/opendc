import PropTypes from 'prop-types';

const Shapes = {};

Shapes.User = PropTypes.shape({
    id: PropTypes.number.isRequired,
    googleId: PropTypes.number.isRequired,
    email: PropTypes.string.isRequired,
    givenName: PropTypes.string.isRequired,
    familyName: PropTypes.string.isRequired,
});

Shapes.Simulation = PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
    datetimeCreated: PropTypes.string.isRequired,
    datetimeLastEdited: PropTypes.string.isRequired,
});

Shapes.Authorization = PropTypes.shape({
    userId: PropTypes.number.isRequired,
    user: Shapes.User,
    simulationId: PropTypes.number.isRequired,
    simulation: Shapes.Simulation,
    authorizationLevel: PropTypes.string.isRequired,
});

export default Shapes;