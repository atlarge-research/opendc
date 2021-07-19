import PropTypes from 'prop-types'
import React from 'react'
import MachineComponent from './MachineComponent'
import {
    Badge,
    Button,
    DataList,
    DataListAction,
    DataListCell,
    DataListItem,
    DataListItemCells,
    DataListItemRow,
} from '@patternfly/react-core'
import { AngleRightIcon, PlusIcon } from '@patternfly/react-icons'
import { Machine } from '../../../../shapes'

function MachineListComponent({ machines = [], onSelect, onAdd }) {
    return (
        <DataList aria-label="Rack Units">
            {machines.map((machine, index) =>
                machine ? (
                    <DataListItem key={index} onClick={() => onSelect(index + 1)}>
                        <DataListItemRow>
                            <DataListItemCells
                                dataListCells={[
                                    <DataListCell isIcon key="icon">
                                        <Badge isRead>{machines.length - index}U</Badge>
                                    </DataListCell>,
                                    <DataListCell key="primary content">
                                        <MachineComponent onClick={() => onSelect(index + 1)} machine={machine} />
                                    </DataListCell>,
                                ]}
                            />
                            <DataListAction id="goto" aria-label="Goto Machine" aria-labelledby="goto">
                                <Button isSmall variant="plain" className="pf-u-p-0">
                                    <AngleRightIcon />
                                </Button>
                            </DataListAction>
                        </DataListItemRow>
                    </DataListItem>
                ) : (
                    <DataListItem key={index}>
                        <DataListItemRow>
                            <DataListItemCells
                                dataListCells={[
                                    <DataListCell isIcon key="icon">
                                        <Badge isRead>{machines.length - index}U</Badge>
                                    </DataListCell>,
                                    <DataListCell key="add" className="text-secondary">
                                        Empty Slot
                                    </DataListCell>,
                                ]}
                            />
                            <DataListAction id="add" aria-label="Add Machine" aria-labelledby="add">
                                <Button isSmall variant="plain" className="pf-u-p-0" onClick={() => onAdd(index + 1)}>
                                    <PlusIcon />
                                </Button>
                            </DataListAction>
                        </DataListItemRow>
                    </DataListItem>
                )
            )}
        </DataList>
    )
}

MachineListComponent.propTypes = {
    machines: PropTypes.arrayOf(Machine),
    onSelect: PropTypes.func.isRequired,
    onAdd: PropTypes.func.isRequired,
}

export default MachineListComponent
