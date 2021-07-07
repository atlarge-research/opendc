import React from 'react'
import PropTypes from 'prop-types'
import ProjectList from '../../components/projects/ProjectList'
import { useAuth } from '../../auth'
import { useProjects } from '../../data/project'
import { useQueryClient } from 'react-query'

const getVisibleProjects = (projects, filter, userId) => {
    switch (filter) {
        case 'SHOW_ALL':
            return projects
        case 'SHOW_OWN':
            return projects.filter((project) =>
                project.authorizations.some((a) => a.userId === userId && a.level === 'OWN')
            )
        case 'SHOW_SHARED':
            return projects.filter((project) =>
                project.authorizations.some((a) => a.userId === userId && a.level !== 'OWN')
            )
        default:
            return projects
    }
}

const ProjectListContainer = ({ filter }) => {
    const { user } = useAuth()
    const { data: projects } = useProjects()
    return <ProjectList projects={getVisibleProjects(projects ?? [], filter, user?.sub)} />
}

ProjectListContainer.propTypes = {
    filter: PropTypes.string.isRequired,
}

export default ProjectListContainer
