import PropTypes from 'prop-types'
import React, { useState } from 'react'
import { Tab, Tabs, TabTitleText } from '@patternfly/react-core'
import UnitAddContainer from './UnitAddContainer'
import UnitListContainer from './UnitListContainer'

function UnitTabsComponent({ machineId }) {
    const [activeTab, setActiveTab] = useState('cpu-units')

    return (
        <Tabs activeKey={activeTab} onSelect={(_, tab) => setActiveTab(tab)}>
            <Tab eventKey="cpu-units" title={<TabTitleText>CPU</TabTitleText>}>
                <UnitAddContainer machineId={machineId} unitType="cpu" />
                <UnitListContainer machineId={machineId} unitType="cpu" />
            </Tab>
            <Tab eventKey="gpu-units" title={<TabTitleText>GPU</TabTitleText>}>
                <UnitAddContainer machineId={machineId} unitType="gpu" />
                <UnitListContainer machineId={machineId} unitType="gpu" />
            </Tab>
            <Tab eventKey="memory-units" title={<TabTitleText>Memory</TabTitleText>}>
                <UnitAddContainer machineId={machineId} unitType="memory" />
                <UnitListContainer machineId={machineId} unitType="memory" />
            </Tab>
            <Tab eventKey="storage-units" title={<TabTitleText>Storage</TabTitleText>}>
                <UnitAddContainer machineId={machineId} unitType="storage" />
                <UnitListContainer machineId={machineId} unitType="storage" />
            </Tab>
        </Tabs>
    )
}

UnitTabsComponent.propTypes = {
    machineId: PropTypes.string.isRequired,
}

export default UnitTabsComponent
