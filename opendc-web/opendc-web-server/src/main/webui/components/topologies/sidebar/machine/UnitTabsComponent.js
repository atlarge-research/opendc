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
                <UnitAddContainer machineId={machineId} unitType="cpus" />
                <UnitListContainer machineId={machineId} unitType="cpus" />
            </Tab>
            <Tab eventKey="gpu-units" title={<TabTitleText>GPU</TabTitleText>}>
                <UnitAddContainer machineId={machineId} unitType="gpus" />
                <UnitListContainer machineId={machineId} unitType="gpus" />
            </Tab>
            <Tab eventKey="memory-units" title={<TabTitleText>Memory</TabTitleText>}>
                <UnitAddContainer machineId={machineId} unitType="memories" />
                <UnitListContainer machineId={machineId} unitType="memories" />
            </Tab>
            <Tab eventKey="storage-units" title={<TabTitleText>Storage</TabTitleText>}>
                <UnitAddContainer machineId={machineId} unitType="storages" />
                <UnitListContainer machineId={machineId} unitType="storages" />
            </Tab>
        </Tabs>
    )
}

UnitTabsComponent.propTypes = {
    machineId: PropTypes.string.isRequired,
}

export default UnitTabsComponent
