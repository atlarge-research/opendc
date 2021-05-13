import React from 'react'
import AppNavbarComponent from '../../components/navigation/AppNavbarComponent'
import { useProject } from '../../store/hooks/project'

const AppNavbarContainer = (props) => {
    const project = useProject()
    return <AppNavbarComponent {...props} project={project} />
}

export default AppNavbarContainer
