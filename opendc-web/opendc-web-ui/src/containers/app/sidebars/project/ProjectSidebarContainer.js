import React from 'react'
import { useRouter } from 'next/router'
import ProjectSidebarComponent from '../../../../components/app/sidebars/project/ProjectSidebarComponent'
import { isCollapsible } from '../../../../util/sidebar-space'

const ProjectSidebarContainer = (props) => {
    const router = useRouter()
    return <ProjectSidebarComponent collapsible={isCollapsible(router)} {...props} />
}

export default ProjectSidebarContainer
