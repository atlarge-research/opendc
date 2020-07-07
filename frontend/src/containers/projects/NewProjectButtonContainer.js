import { connect } from 'react-redux'
import { openNewProjectModal } from '../../actions/modals/projects'
import NewProjectButtonComponent from '../../components/projects/NewProjectButtonComponent'

const mapDispatchToProps = dispatch => {
    return {
        onClick: () => dispatch(openNewProjectModal()),
    }
}

const NewProjectButtonContainer = connect(undefined, mapDispatchToProps)(
    NewProjectButtonComponent,
)

export default NewProjectButtonContainer
