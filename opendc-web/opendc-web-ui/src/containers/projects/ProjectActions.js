import { connect } from 'react-redux'
import { deleteProject } from '../../actions/projects'
import ProjectActionButtons from '../../components/projects/ProjectActionButtons'

const mapStateToProps = (state, ownProps) => {
    return {
        projectId: ownProps.projectId,
    }
}

const mapDispatchToProps = (dispatch) => {
    return {
        onViewUsers: (id) => {}, // TODO implement user viewing
        onDelete: (id) => dispatch(deleteProject(id)),
    }
}

const ProjectActions = connect(mapStateToProps, mapDispatchToProps)(ProjectActionButtons)

export default ProjectActions
