import {connect} from "react-redux";
import {deleteProject, openProject} from "../../actions/projects";
import ProjectActionButtons from "../../components/projects/ProjectActionButtons";

const mapStateToProps = (state, ownProps) => {
    return {
        projectId: ownProps.projectId
    };
};

const mapDispatchToProps = dispatch => {
    return {
        onOpen: (id) => dispatch(openProject(id)),
        onViewUsers: (id) => {},
        onDelete: (id) => dispatch(deleteProject(id)),
    };
};

const ProjectActions = connect(
    mapStateToProps,
    mapDispatchToProps
)(ProjectActionButtons);

export default ProjectActions;
