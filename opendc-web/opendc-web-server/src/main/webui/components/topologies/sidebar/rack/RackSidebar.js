import PropTypes from 'prop-types'
import React from 'react'
import { machineListContainer, sidebarContainer } from './RackSidebar.module.css'
import RackClusterNameContainer from './RackClusterNameContainer'
import RackNameContainer from './RackNameContainer'
import AddPrefab from './AddPrefab'
import DeleteRackContainer from './DeleteRackContainer'
import MachineListContainer from './MachineListContainer'
import NameComponent from '../NameComponent'
import {
    Skeleton,
    TextContent,
    TextList,
    TextListItem,
    TextListItemVariants,
    TextListVariants,
    Title,
} from '@patternfly/react-core'
import { useDispatch, useSelector } from 'react-redux'
import { editRackPowerCapacity } from '../../../../redux/actions/topology/rack'

function RackSidebar({ tileId }) {
    const rack = useSelector((state) => state.topology.racks[state.topology.tiles[tileId].rack])
    const dispatch = useDispatch()

    return (
        <div className={sidebarContainer}>
            <TextContent>
                <Title headingLevel="h2" ouiaId="rack-details-title">Details</Title>
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
                    <TextListItem
                        component={TextListItemVariants.dt}
                        className="pf-u-display-inline-flex pf-u-align-items-center"
                    >
                        Cluster
                    </TextListItem>
                    <TextListItem component={TextListItemVariants.dd}>
                        <RackClusterNameContainer tileId={tileId} />
                    </TextListItem>
                    <TextListItem component={TextListItemVariants.dt}>Capacity</TextListItem>
                    <TextListItem component={TextListItemVariants.dd}>
                        {rack?.capacity ?? <Skeleton screenreaderText="Loading rack" />}
                    </TextListItem>
                    <TextListItem
                        component={TextListItemVariants.dt}
                        className="pf-u-display-inline-flex pf-u-align-items-center"
                    >
                        Power capacity
                    </TextListItem>
                    <TextListItem component={TextListItemVariants.dd}>
                        {rack ? (
                            <NameComponent
                                name={`${rack.powerCapacityW} W`}
                                onEdit={(val) => {
                                    const parsed = parseFloat(val)
                                    if (!isNaN(parsed) && parsed > 0) {
                                        dispatch(editRackPowerCapacity(rack.id, parsed))
                                    }
                                }}
                            />
                        ) : (
                            <Skeleton screenreaderText="Loading rack" />
                        )}
                    </TextListItem>
                </TextList>
                <Title headingLevel="h2" ouiaId="rack-actions-title">Actions</Title>
                <AddPrefab tileId={tileId} />
                <DeleteRackContainer tileId={tileId} />

                <Title headingLevel="h2" ouiaId="rack-slots-title">Slots</Title>
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
