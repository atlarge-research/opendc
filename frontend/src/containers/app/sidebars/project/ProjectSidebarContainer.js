import React from 'react'
import { withRouter } from 'react-router-dom'
import ProjectSidebarComponent from '../../../../components/app/sidebars/project/ProjectSidebarComponent'
import { isCollapsible } from '../../../../util/sidebar-space'

const ProjectSidebarContainer = withRouter(({ location, ...props }) => (
    <ProjectSidebarComponent collapsible={isCollapsible(location)} {...props} />
))

export default ProjectSidebarContainer
