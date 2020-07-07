import React from 'react'
import DocumentTitle from 'react-document-title'
import { connect } from 'react-redux'
import { openNewProjectModal } from '../actions/modals/projects'
import { fetchAuthorizationsOfCurrentUser } from '../actions/users'
import AppNavbar from '../components/navigation/AppNavbar'
import ProjectFilterPanel from '../components/projects/FilterPanel'
import NewProjectModal from '../containers/modals/NewProjectModal'
import NewProjectButtonContainer from '../containers/projects/NewProjectButtonContainer'
import VisibleProjectList from '../containers/projects/VisibleProjectAuthList'

class ProjectsContainer extends React.Component {
    componentDidMount() {
        this.props.fetchAuthorizationsOfCurrentUser()
    }

    render() {
        return (
            <DocumentTitle title="My Projects - OpenDC">
                <div className="full-height">
                    <AppNavbar inProject={false} fullWidth={false}/>
                    <div className="container text-page-container full-height">
                        <ProjectFilterPanel/>
                        <VisibleProjectList/>
                        <NewProjectButtonContainer/>
                    </div>
                    <NewProjectModal/>
                </div>
            </DocumentTitle>
        )
    }
}

const mapDispatchToProps = (dispatch) => {
    return {
        fetchAuthorizationsOfCurrentUser: () => dispatch(fetchAuthorizationsOfCurrentUser()),
        openNewProjectModal: () => dispatch(openNewProjectModal()),
    }
}

const Projects = connect(undefined, mapDispatchToProps)(ProjectsContainer)

export default Projects
