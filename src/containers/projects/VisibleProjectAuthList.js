import {connect} from "react-redux";
import ProjectList from "../../components/projects/ProjectAuthList";

const getVisibleProjectAuths = (projectAuths, filter) => {
    switch (filter) {
        case 'SHOW_ALL':
            return projectAuths;
        case 'SHOW_OWN':
            return projectAuths.filter(projectAuth => projectAuth.authorizationLevel === "OWN");
        case 'SHOW_SHARED':
            return projectAuths.filter(projectAuth => projectAuth.authorizationLevel !== "OWN");
        default:
            return projectAuths;
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
        authorizations: getVisibleProjectAuths(denormalizedAuthorizations, state.authVisibilityFilter)
    };
};

const VisibleProjectAuthList = connect(mapStateToProps)(ProjectList);

export default VisibleProjectAuthList;
