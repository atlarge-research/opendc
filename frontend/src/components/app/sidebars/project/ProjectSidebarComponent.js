import React from 'react'
import Sidebar from '../Sidebar'
import TopologyListContainer from '../../../../containers/app/sidebars/project/TopologyListContainer'
import PortfolioListContainer from '../../../../containers/app/sidebars/project/PortfolioListContainer'

const ProjectSidebarComponent = ({ collapsible }) => (
    <Sidebar isRight={false} collapsible={collapsible}>
        <div className="h-100 overflow-auto container-fluid">
            <TopologyListContainer />
            <PortfolioListContainer />
        </div>
    </Sidebar>
)

export default ProjectSidebarComponent
