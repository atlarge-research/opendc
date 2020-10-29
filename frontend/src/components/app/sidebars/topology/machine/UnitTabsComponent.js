import React, { useState } from 'react'
import { Nav, NavItem, NavLink, TabContent, TabPane } from 'reactstrap'
import UnitAddContainer from '../../../../../containers/app/sidebars/topology/machine/UnitAddContainer'
import UnitListContainer from '../../../../../containers/app/sidebars/topology/machine/UnitListContainer'

const UnitTabsComponent = () => {
    const [activeTab, setActiveTab] = useState('cpu-units')
    const toggle = (tab) => {
        if (activeTab !== tab) setActiveTab(tab)
    }

    return (
        <div>
            <Nav tabs>
                <NavItem>
                    <NavLink
                        className={activeTab === 'cpu-units' && 'active'}
                        onClick={() => {
                            toggle('cpu-units')
                        }}
                    >
                        CPU
                    </NavLink>
                </NavItem>
                <NavItem>
                    <NavLink
                        className={activeTab === 'gpu-units' && 'active'}
                        onClick={() => {
                            toggle('gpu-units')
                        }}
                    >
                        GPU
                    </NavLink>
                </NavItem>
                <NavItem>
                    <NavLink
                        className={activeTab === 'memory-units' && 'active'}
                        onClick={() => {
                            toggle('memory-units')
                        }}
                    >
                        Memory
                    </NavLink>
                </NavItem>
                <NavItem>
                    <NavLink
                        className={activeTab === 'storage-units' && 'active'}
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
                    <UnitAddContainer unitType="cpu" />
                    <UnitListContainer unitType="cpu" />
                </TabPane>
                <TabPane tabId="gpu-units">
                    <UnitAddContainer unitType="gpu" />
                    <UnitListContainer unitType="gpu" />
                </TabPane>
                <TabPane tabId="memory-units">
                    <UnitAddContainer unitType="memory" />
                    <UnitListContainer unitType="memory" />
                </TabPane>
                <TabPane tabId="storage-units">
                    <UnitAddContainer unitType="storage" />
                    <UnitListContainer unitType="storage" />
                </TabPane>
            </TabContent>
        </div>
    )
}

export default UnitTabsComponent
