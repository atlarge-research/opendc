import PropTypes from 'prop-types'
import React from 'react'
import UnitTabsComponent from './UnitTabsComponent'
import DeleteMachine from './DeleteMachine'
import {
    TextContent,
    TextList,
    TextListItem,
    TextListItemVariants,
    TextListVariants,
    Title,
} from '@patternfly/react-core'
import { useSelector } from 'react-redux'

function MachineSidebar({ tileId, position }) {
    const machine = useSelector(({ topology }) => {
        const rack = topology.racks[topology.tiles[tileId].rack]
        return topology.machines[rack.machines[position - 1]]
    })
    const machineId = machine.id
    return (
        <div>
            <TextContent>
                <Title headingLevel="h2">Details</Title>
                <TextList component={TextListVariants.dl}>
                    <TextListItem component={TextListItemVariants.dt}>Name</TextListItem>
                    <TextListItem component={TextListItemVariants.dd}>
                        Machine at position {machine.position}
                    </TextListItem>
                </TextList>

                <Title headingLevel="h2">Actions</Title>
                <DeleteMachine machineId={machineId} />

                <Title headingLevel="h2">Units</Title>
            </TextContent>
            <div className="pf-u-h-100">
                <UnitTabsComponent machineId={machineId} />
            </div>
        </div>
    )
}

MachineSidebar.propTypes = {
    tileId: PropTypes.string.isRequired,
    position: PropTypes.number.isRequired,
}

export default MachineSidebar
