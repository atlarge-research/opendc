import React from 'react'
import Sidebar from '../Sidebar'
import TopologyListContainer from '../../../../containers/app/sidebars/project/TopologyListContainer'
import PortfolioListContainer from '../../../../containers/app/sidebars/project/PortfolioListContainer'
import { Container } from 'reactstrap'

const ProjectSidebarComponent = ({ collapsible }) => (
    <Sidebar isRight={false} collapsible={collapsible}>
        <Container fluid className="h-100 overflow-auto">
            <TopologyListContainer />
            <PortfolioListContainer />
        </Container>
    </Sidebar>
)

export default ProjectSidebarComponent
