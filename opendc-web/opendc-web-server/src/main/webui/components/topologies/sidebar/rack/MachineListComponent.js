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
    FormSelect,
    FormSelectOption,
} from '@patternfly/react-core'
import { AngleRightIcon, PlusIcon } from '@patternfly/react-icons'
import { Machine } from '../../../../shapes'

function EmptySlot({ index, prefabs, onAdd }) {
    const [selectedPrefabId, setSelectedPrefabId] = React.useState('')

    const selectedPrefab = prefabs.find((p) => p.id === parseInt(selectedPrefabId)) ?? null

    return (
        <DataListItem key={index}>
            <DataListItemRow>
                <DataListItemCells
                    dataListCells={[
                        <DataListCell isIcon key="icon">
                            <Badge isRead>{index + 1}U</Badge>
                        </DataListCell>,
                        <DataListCell key="add">
                            {prefabs.length > 0 ? (
                                <FormSelect
                                    value={selectedPrefabId}
                                    onChange={(value) => setSelectedPrefabId(value)}
                                    aria-label="Select machine prefab"
                                    ouiaId={`machine-prefab-select-${index}`}
                                >
                                    <FormSelectOption key="" value="" label="Empty Machine" />
                                    {prefabs.map((prefab) => (
                                        <FormSelectOption key={prefab.id} value={prefab.id} label={prefab.name} />
                                    ))}
                                </FormSelect>
                            ) : (
                                <span className="text-secondary">Empty Slot</span>
                            )}
                        </DataListCell>,
                    ]}
                />
                <DataListAction id="add" aria-label="Add Machine" aria-labelledby="add">
                    <Button
                        isSmall
                        variant="plain"
                        className="pf-u-p-0"
                        ouiaId={`machine-add-${index}`}
                        onClick={() => onAdd(index + 1, selectedPrefab)}
                    >
                        <PlusIcon />
                    </Button>
                </DataListAction>
            </DataListItemRow>
        </DataListItem>
    )
}

EmptySlot.propTypes = {
    index: PropTypes.number.isRequired,
    prefabs: PropTypes.array.isRequired,
    onAdd: PropTypes.func.isRequired,
}

function MachineListComponent({ machines = [], prefabs = [], onSelect, onAdd }) {
    return (
        <DataList aria-label="Rack Units">
            {machines
                .map((machine, index) =>
                    machine ? (
                        <DataListItem key={index} onClick={() => onSelect(index + 1)}>
                            <DataListItemRow>
                                <DataListItemCells
                                    dataListCells={[
                                        <DataListCell isIcon key="icon">
                                            <Badge isRead>{index + 1}U</Badge>
                                        </DataListCell>,
                                        <DataListCell key="primary content">
                                            <MachineComponent onClick={() => onSelect(index + 1)} machine={machine} />
                                        </DataListCell>,
                                    ]}
                                />
                                <DataListAction id="goto" aria-label="Goto Machine" aria-labelledby="goto">
                                    <Button isSmall variant="plain" className="pf-u-p-0" ouiaId={`machine-goto-${index}`}>
                                        <AngleRightIcon />
                                    </Button>
                                </DataListAction>
                            </DataListItemRow>
                        </DataListItem>
                    ) : (
                        <EmptySlot key={index} index={index} prefabs={prefabs} onAdd={onAdd} />
                    )
                )
                .reverse()}
        </DataList>
    )
}

MachineListComponent.propTypes = {
    machines: PropTypes.arrayOf(Machine),
    prefabs: PropTypes.array,
    onSelect: PropTypes.func.isRequired,
    onAdd: PropTypes.func.isRequired,
}

export default MachineListComponent
