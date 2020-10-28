import React from 'react'
import { useLocation } from 'react-router-dom'
import ProjectSidebarComponent from '../../../../components/app/sidebars/project/ProjectSidebarComponent'
import { isCollapsible } from '../../../../util/sidebar-space'

const ProjectSidebarContainer = (props) => {
    const location = useLocation()
    return <ProjectSidebarComponent collapsible={isCollapsible(location)} {...props} />
}

export default ProjectSidebarContainer
