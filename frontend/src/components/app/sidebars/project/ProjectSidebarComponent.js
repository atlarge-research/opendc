import React from 'react'
import Sidebar from '../Sidebar'
import TopologyListContainer from '../../../../containers/app/sidebars/project/TopologyListContainer'
import PortfolioListContainer from '../../../../containers/app/sidebars/project/PortfolioListContainer'

const ProjectSidebarComponent = ({collapsible}) => (
        <Sidebar isRight={false} collapsible={collapsible}>
            <TopologyListContainer/>
            <PortfolioListContainer/>
        </Sidebar>
    )

export default ProjectSidebarComponent
