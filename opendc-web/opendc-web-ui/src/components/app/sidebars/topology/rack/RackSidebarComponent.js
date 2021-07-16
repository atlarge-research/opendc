import PropTypes from 'prop-types'
import React from 'react'
import { machineListContainer, sidebarContainer } from './RackSidebarComponent.module.scss'
import RackNameContainer from './RackNameContainer'
import AddPrefabContainer from './AddPrefabContainer'
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

function RackSidebarComponent({ tileId }) {
    const rack = useSelector((state) => state.objects.rack[state.objects.tile[tileId].rack])

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
                <AddPrefabContainer />
                <DeleteRackContainer />

                <Title headingLevel="h2">Slots</Title>
            </TextContent>
            <div className={machineListContainer}>
                <MachineListContainer tileId={tileId} />
            </div>
        </div>
    )
}

RackSidebarComponent.propTypes = {
    tileId: PropTypes.string.isRequired,
}

export default RackSidebarComponent
