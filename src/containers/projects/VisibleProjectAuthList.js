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
    return {
        authorizations: getVisibleProjectAuths(state.authorizations, state.authVisibilityFilter)
    };
};

const VisibleProjectAuthList = connect(mapStateToProps)(ProjectList);

export default VisibleProjectAuthList;
