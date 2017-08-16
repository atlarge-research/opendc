import {connect} from "react-redux";
import SimulationList from "../../components/simulations/SimulationAuthList";

const getVisibleSimulationAuths = (simulationAuths, filter) => {
    switch (filter) {
        case 'SHOW_ALL':
            return simulationAuths;
        case 'SHOW_OWN':
            return simulationAuths.filter(simulationAuth => simulationAuth.authorizationLevel === "OWN");
        case 'SHOW_SHARED':
            return simulationAuths.filter(simulationAuth => simulationAuth.authorizationLevel !== "OWN");
        default:
            return simulationAuths;
    }
};

const mapStateToProps = state => {
    const denormalizedAuthorizations = state.authorizationsOfCurrentUser.map(authorizationIds => {
        const authorization = Object.assign({}, state.objects.authorizations[authorizationIds]);
        authorization.simulation = state.objects.simulations[authorization.simulationId];
        authorization.user = state.objects.users[authorization.userId];
        return authorization;
    });

    return {
        authorizations: getVisibleSimulationAuths(denormalizedAuthorizations, state.authVisibilityFilter)
    };
};

const VisibleSimulationAuthList = connect(mapStateToProps)(SimulationList);

export default VisibleSimulationAuthList;
