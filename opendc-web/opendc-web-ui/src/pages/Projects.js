import React, { useEffect } from 'react'
import DocumentTitle from 'react-document-title'
import { useDispatch } from 'react-redux'
import { fetchAuthorizationsOfCurrentUser } from '../actions/users'
import ProjectFilterPanel from '../components/projects/FilterPanel'
import NewProjectModal from '../containers/modals/NewProjectModal'
import NewProjectButtonContainer from '../containers/projects/NewProjectButtonContainer'
import VisibleProjectList from '../containers/projects/VisibleProjectAuthList'
import AppNavbarContainer from '../containers/navigation/AppNavbarContainer'

function Projects() {
    const dispatch = useDispatch()

    useEffect(() => dispatch(fetchAuthorizationsOfCurrentUser()))

    return (
        <DocumentTitle title="My Projects - OpenDC">
            <div className="full-height">
                <AppNavbarContainer fullWidth={false} />
                <div className="container text-page-container full-height">
                    <ProjectFilterPanel />
                    <VisibleProjectList />
                    <NewProjectButtonContainer />
                </div>
                <NewProjectModal />
            </div>
        </DocumentTitle>
    )
}

export default Projects
