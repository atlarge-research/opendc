import React from 'react'
import { useSelector } from 'react-redux'
import AppNavbarComponent from '../../components/navigation/AppNavbarComponent'

const AppNavbarContainer = (props) => {
    const project = useSelector((state) =>
        state.currentProjectId !== '-1' ? state.objects.project[state.currentProjectId] : undefined
    )
    return <AppNavbarComponent {...props} project={project} />
}

export default AppNavbarContainer
