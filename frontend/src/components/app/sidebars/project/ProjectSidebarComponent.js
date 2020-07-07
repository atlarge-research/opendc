import React from 'react'
import Sidebar from '../Sidebar'
import TopologyListContainer from '../../../../containers/app/sidebars/project/TopologyListContainer'

const ProjectSidebarComponent = () => (
        <Sidebar isRight={false}>
            <TopologyListContainer/>
            <h2>Portfolios</h2>
        </Sidebar>
    )

export default ProjectSidebarComponent
