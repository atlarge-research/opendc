import PropTypes from 'prop-types'
import React from 'react'
import { machineListContainer, sidebarContainer } from './RackSidebar.module.css'
import RackNameContainer from './RackNameContainer'
import AddPrefab from './AddPrefab'
import DeleteRackContainer from './DeleteRackContainer'
import MachineListContainer from './MachineListContainer'
import {
    Skeleton,
    TextContent,
    TextList,
    TextListItem,
    TextListItemVariants,
    TextListVariants,
    Title,
} from '@patternfly/react-core'
import { useSelector } from 'react-redux'

function RackSidebar({ tileId }) {
    const rack = useSelector((state) => state.topology.racks[state.topology.tiles[tileId].rack])

    return (
        <div className={sidebarContainer}>
            <TextContent>
                <Title headingLevel="h2">Details</Title>
                <TextList component={TextListVariants.dl}>
                    <TextListItem
                        component={TextListItemVariants.dt}
                        className="pf-u-display-inline-flex pf-u-align-items-center"
                    >
                        Name
                    </TextListItem>
                    <TextListItem component={TextListItemVariants.dd}>
                        <RackNameContainer tileId={tileId} />
                    </TextListItem>
                    <TextListItem component={TextListItemVariants.dt}>Capacity</TextListItem>
                    <TextListItem component={TextListItemVariants.dd}>
                        {rack?.capacity ?? <Skeleton screenreaderText="Loading rack" />}
                    </TextListItem>
                </TextList>
                <Title headingLevel="h2">Actions</Title>
                <AddPrefab tileId={tileId} />
                <DeleteRackContainer tileId={tileId} />

                <Title headingLevel="h2">Slots</Title>
            </TextContent>
            <div className={machineListContainer}>
                <MachineListContainer tileId={tileId} />
            </div>
        </div>
    )
}

RackSidebar.propTypes = {
    tileId: PropTypes.string.isRequired,
}

export default RackSidebar
