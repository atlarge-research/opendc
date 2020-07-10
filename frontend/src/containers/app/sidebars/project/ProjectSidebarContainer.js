import React from 'react'
import { withRouter } from 'react-router-dom'
import ProjectSidebarComponent from '../../../../components/app/sidebars/project/ProjectSidebarComponent'

const ProjectSidebarContainer = withRouter(({ location, ...props }) => (
    <ProjectSidebarComponent
        collapsible={location.pathname.indexOf('portfolios') === -1 && location.pathname.indexOf('scenarios') === -1} {...props}/>
))

export default ProjectSidebarContainer
