import React from 'react'
import AppNavbarComponent from '../../components/navigation/AppNavbarComponent'
import { useActiveProject } from '../../store/hooks/project'

const AppNavbarContainer = (props) => {
    const project = useActiveProject()
    return <AppNavbarComponent {...props} project={project} />
}

export default AppNavbarContainer
