import React, { useEffect, useState } from 'react'
import Head from 'next/head'
import { useDispatch } from 'react-redux'
import { fetchAuthorizationsOfCurrentUser } from '../../redux/actions/users'
import ProjectFilterPanel from '../../components/projects/FilterPanel'
import NewProjectContainer from '../../containers/projects/NewProjectContainer'
import VisibleProjectList from '../../containers/projects/VisibleProjectAuthList'
import AppNavbarContainer from '../../containers/navigation/AppNavbarContainer'
import { useRequireAuth } from '../../auth'
import { Container } from 'reactstrap'

function Projects() {
    useRequireAuth()

    const dispatch = useDispatch()
    const [filter, setFilter] = useState('SHOW_ALL')

    useEffect(() => dispatch(fetchAuthorizationsOfCurrentUser()), [])

    return (
        <>
            <Head>
                <title>My Projects - OpenDC</title>
            </Head>
            <div className="full-height">
                <AppNavbarContainer fullWidth={false} />
                <Container className="text-page-container">
                    <ProjectFilterPanel onSelect={setFilter} activeFilter={filter} />
                    <VisibleProjectList filter={filter} />
                    <NewProjectContainer />
                </Container>
            </div>
        </>
    )
}

export default Projects
