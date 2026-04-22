import PropTypes from 'prop-types'
import React from 'react'
import UnitTabsComponent from './UnitTabsComponent'
import AddMachinePrefab from './AddMachinePrefab'
import DeleteMachine from './DeleteMachine'
import NameComponent from '../NameComponent'
import {
    TextContent,
    TextList,
    TextListItem,
    TextListItemVariants,
    TextListVariants,
    Title,
} from '@patternfly/react-core'
import { useDispatch, useSelector } from 'react-redux'
import { editMachineName } from '../../../../redux/actions/topology/machine'

function MachineSidebar({ tileId, position }) {
    const machine = useSelector(({ topology }) => {
        const rack = topology.racks[topology.tiles[tileId].rack]

        for (const machineId of rack.machines) {
            const machine = topology.machines[machineId]
            if (machine.position === position) {
                return machine
            }
        }
    })
    const machineId = machine.id
    const dispatch = useDispatch()
    return (
        <div>
            <TextContent>
                <Title headingLevel="h2" ouiaId="machine-details-title">Details</Title>
                <TextList component={TextListVariants.dl}>
                    <TextListItem component={TextListItemVariants.dt}>Name</TextListItem>
                    <TextListItem component={TextListItemVariants.dd}>
                        <NameComponent
                            name={machine.name ?? `Machine at position ${machine.position}`}
                            onEdit={(name) => dispatch(editMachineName(machine.id, name))}
                        />
                    </TextListItem>
                </TextList>

                <Title headingLevel="h2" ouiaId="machine-actions-title">Actions</Title>
                <AddMachinePrefab tileId={tileId} position={position} />
                <DeleteMachine machineId={machineId} />

                <Title headingLevel="h2" ouiaId="machine-units-title">Units</Title>
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
