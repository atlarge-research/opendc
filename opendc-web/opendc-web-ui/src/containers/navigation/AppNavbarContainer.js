import React from 'react'
import AppNavbarComponent from '../../components/navigation/AppNavbarComponent'
import { useActiveProjectId, useProject } from '../../data/project'

const AppNavbarContainer = (props) => {
    const projectId = useActiveProjectId()
    const { data: project } = useProject(projectId)
    return <AppNavbarComponent {...props} project={project} />
}

export default AppNavbarContainer
