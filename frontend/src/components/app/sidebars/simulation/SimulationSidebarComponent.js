import React from 'react'
import ExperimentMetadataContainer from '../../../../containers/app/sidebars/simulation/ExperimentMetadataContainer'
import LoadMetricContainer from '../../../../containers/app/sidebars/simulation/LoadMetricContainer'
import TraceContainer from '../../../../containers/app/sidebars/simulation/TraceContainer'
import Sidebar from '../Sidebar'
import './SimulationSidebarComponent.css'

const SimulationSidebarComponent = () => {
    return (
        <Sidebar isRight={false}>
            <div className="simulation-sidebar-container flex-column">
                <ExperimentMetadataContainer/>
                <LoadMetricContainer/>
                <div className="trace-container">
                    <TraceContainer/>
                </div>
            </div>
        </Sidebar>
    )
}

export default SimulationSidebarComponent
