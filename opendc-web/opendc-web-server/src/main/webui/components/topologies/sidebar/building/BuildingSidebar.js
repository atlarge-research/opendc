import React from 'react'
import { TextContent, Title } from '@patternfly/react-core'
import DatacenterListContainer from './DatacenterListContainer'
import NewDatacenterContainer from './NewDatacenterContainer'

function BuildingSidebar() {
    return (
        <TextContent>
            <DatacenterListContainer />
            <Title headingLevel="h2" ouiaId="building-new-datacenter-title">New Datacenter</Title>
            <NewDatacenterContainer />
        </TextContent>
    )
}

export default BuildingSidebar
