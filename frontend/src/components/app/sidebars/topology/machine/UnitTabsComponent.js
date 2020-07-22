import React from 'react'
import UnitAddContainer from '../../../../../containers/app/sidebars/topology/machine/UnitAddContainer'
import UnitListContainer from '../../../../../containers/app/sidebars/topology/machine/UnitListContainer'

const UnitTabsComponent = () => (
    <div>
        <ul className="nav nav-tabs mt-2 mb-1" role="tablist">
            <li className="nav-item">
                <a className="nav-link active" data-toggle="tab" href="#cpu-units" role="tab">
                    CPU
                </a>
            </li>
            <li className="nav-item">
                <a className="nav-link" data-toggle="tab" href="#gpu-units" role="tab">
                    GPU
                </a>
            </li>
            <li className="nav-item">
                <a className="nav-link" data-toggle="tab" href="#memory-units" role="tab">
                    Memory
                </a>
            </li>
            <li className="nav-item">
                <a className="nav-link" data-toggle="tab" href="#storage-units" role="tab">
                    Storage
                </a>
            </li>
        </ul>
        <div className="tab-content">
            <div className="tab-pane active" id="cpu-units" role="tabpanel">
                <UnitAddContainer unitType="cpu" />
                <UnitListContainer unitType="cpu" />
            </div>
            <div className="tab-pane" id="gpu-units" role="tabpanel">
                <UnitAddContainer unitType="gpu" />
                <UnitListContainer unitType="gpu" />
            </div>
            <div className="tab-pane" id="memory-units" role="tabpanel">
                <UnitAddContainer unitType="memory" />
                <UnitListContainer unitType="memory" />
            </div>
            <div className="tab-pane" id="storage-units" role="tabpanel">
                <UnitAddContainer unitType="storage" />
                <UnitListContainer unitType="storage" />
            </div>
        </div>
    </div>
)

export default UnitTabsComponent
