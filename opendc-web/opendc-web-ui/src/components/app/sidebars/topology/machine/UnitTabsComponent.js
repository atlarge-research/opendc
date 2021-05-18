import React, { useState } from 'react'
import { Nav, NavItem, NavLink, Row, TabContent, TabPane } from 'reactstrap'
import UnitAddContainer from '../../../../../containers/app/sidebars/topology/machine/UnitAddContainer'
import UnitListContainer from '../../../../../containers/app/sidebars/topology/machine/UnitListContainer'

const UnitTabsComponent = () => {
    const [activeTab, setActiveTab] = useState('cpu-units')
    const toggle = (tab) => {
        if (activeTab !== tab) setActiveTab(tab)
    }

    return (
        <div className="mt-2">
            <Nav tabs>
                <NavItem>
                    <NavLink
                        className={activeTab === 'cpu-units' ? 'active' : ''}
                        onClick={() => {
                            toggle('cpu-units')
                        }}
                    >
                        CPU
                    </NavLink>
                </NavItem>
                <NavItem>
                    <NavLink
                        className={activeTab === 'gpu-units' ? 'active' : ''}
                        onClick={() => {
                            toggle('gpu-units')
                        }}
                    >
                        GPU
                    </NavLink>
                </NavItem>
                <NavItem>
                    <NavLink
                        className={activeTab === 'memory-units' ? 'active' : ''}
                        onClick={() => {
                            toggle('memory-units')
                        }}
                    >
                        Memory
                    </NavLink>
                </NavItem>
                <NavItem>
                    <NavLink
                        className={activeTab === 'storage-units' ? 'active' : ''}
                        onClick={() => {
                            toggle('storage-units')
                        }}
                    >
                        Stor.
                    </NavLink>
                </NavItem>
            </Nav>
            <TabContent activeTab={activeTab}>
                <TabPane tabId="cpu-units">
                    <div className="py-2">
                        <UnitAddContainer unitType="cpu" />
                        <UnitListContainer unitType="cpu" />
                    </div>
                </TabPane>
                <TabPane tabId="gpu-units">
                    <div className="py-2">
                        <UnitAddContainer unitType="gpu" />
                        <UnitListContainer unitType="gpu" />
                    </div>
                </TabPane>
                <TabPane tabId="memory-units">
                    <div className="py-2">
                        <UnitAddContainer unitType="memory" />
                        <UnitListContainer unitType="memory" />
                    </div>
                </TabPane>
                <TabPane tabId="storage-units">
                    <div className="py-2">
                        <UnitAddContainer unitType="storage" />
                        <UnitListContainer unitType="storage" />
                    </div>
                </TabPane>
            </TabContent>
        </div>
    )
}

export default UnitTabsComponent
