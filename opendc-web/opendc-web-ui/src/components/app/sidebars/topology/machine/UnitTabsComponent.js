import React, { useState } from 'react'
import { Tab, Tabs, TabTitleText } from '@patternfly/react-core'
import UnitAddContainer from './UnitAddContainer'
import UnitListContainer from './UnitListContainer'

const UnitTabsComponent = () => {
    const [activeTab, setActiveTab] = useState('cpu-units')

    return (
        <Tabs activeKey={activeTab} onSelect={(_, tab) => setActiveTab(tab)}>
            <Tab eventKey="cpu-units" title={<TabTitleText>CPU</TabTitleText>}>
                <UnitAddContainer unitType="cpu" />
                <UnitListContainer unitType="cpu" />
            </Tab>
            <Tab eventKey="gpu-units" title={<TabTitleText>GPU</TabTitleText>}>
                <UnitAddContainer unitType="gpu" />
                <UnitListContainer unitType="gpu" />
            </Tab>
            <Tab eventKey="memory-units" title={<TabTitleText>Memory</TabTitleText>}>
                <UnitAddContainer unitType="memory" />
                <UnitListContainer unitType="memory" />
            </Tab>
            <Tab eventKey="storage-units" title={<TabTitleText>Storage</TabTitleText>}>
                <UnitAddContainer unitType="storage" />
                <UnitListContainer unitType="storage" />
            </Tab>
        </Tabs>
    )
}

export default UnitTabsComponent
