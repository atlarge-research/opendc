import React, { useEffect } from 'react'
import Head from 'next/head'
import { useDispatch } from 'react-redux'
import { fetchAuthorizationsOfCurrentUser } from '../../actions/users'
import ProjectFilterPanel from '../../components/projects/FilterPanel'
import NewProjectModal from '../../containers/modals/NewProjectModal'
import NewProjectButtonContainer from '../../containers/projects/NewProjectButtonContainer'
import VisibleProjectList from '../../containers/projects/VisibleProjectAuthList'
import AppNavbarContainer from '../../containers/navigation/AppNavbarContainer'
import { useRequireAuth } from '../../auth/hook'

function Projects() {
    const dispatch = useDispatch()

    useRequireAuth()
    useEffect(() => dispatch(fetchAuthorizationsOfCurrentUser()))

    return (
        <>
            <Head>
                <title>My Projects - OpenDC</title>
            </Head>
            <div className="full-height">
                <AppNavbarContainer fullWidth={false} />
                <div className="container text-page-container full-height">
                    <ProjectFilterPanel />
                    <VisibleProjectList />
                    <NewProjectButtonContainer />
                </div>
                <NewProjectModal />
            </div>
        </>
    )
}

export default Projects
