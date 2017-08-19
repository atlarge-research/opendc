import {connect} from "react-redux";
import SimulationList from "../../components/simulations/SimulationAuthList";
import {denormalize} from "../../store/denormalizer";

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
    const denormalizedAuthorizations = state.authorizationsOfCurrentUser.map(authorizationIds =>
        denormalize(state, "authorization", authorizationIds)
    );

    return {
        authorizations: getVisibleSimulationAuths(denormalizedAuthorizations, state.authVisibilityFilter)
    };
};

const VisibleSimulationAuthList = connect(mapStateToProps)(SimulationList);

export default VisibleSimulationAuthList;
