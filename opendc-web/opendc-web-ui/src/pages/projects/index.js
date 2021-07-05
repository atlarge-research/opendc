import React, { useEffect, useState } from 'react'
import Head from 'next/head'
import { useDispatch } from 'react-redux'
import ProjectFilterPanel from '../../components/projects/FilterPanel'
import NewProjectContainer from '../../containers/projects/NewProjectContainer'
import ProjectListContainer from '../../containers/projects/ProjectListContainer'
import AppNavbarContainer from '../../containers/navigation/AppNavbarContainer'
import { useRequireAuth } from '../../auth'
import { Container } from 'reactstrap'
import { fetchProjects } from '../../redux/actions/projects'

function Projects() {
    useRequireAuth()

    const dispatch = useDispatch()
    const [filter, setFilter] = useState('SHOW_ALL')

    useEffect(() => dispatch(fetchProjects()), [dispatch])

    return (
        <>
            <Head>
                <title>My Projects - OpenDC</title>
            </Head>
            <div className="full-height">
                <AppNavbarContainer fullWidth={false} />
                <Container className="text-page-container">
                    <ProjectFilterPanel onSelect={setFilter} activeFilter={filter} />
                    <ProjectListContainer filter={filter} />
                    <NewProjectContainer />
                </Container>
            </div>
        </>
    )
}

export default Projects
