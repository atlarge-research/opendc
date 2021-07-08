import PropTypes from 'prop-types'
import React from 'react'
import { machineList } from './MachineListComponent.module.scss'
import MachineComponent from './MachineComponent'
import { Machine } from '../../../../../shapes'
import EmptySlotComponent from './EmptySlotComponent'

const MachineListComponent = ({ machines = [], onSelect, onAdd }) => {
    return (
        <ul className={`list-group ${machineList}`}>
            {machines.map((machine, index) => {
                if (machine === null) {
                    return <EmptySlotComponent key={index} onAdd={() => onAdd(index + 1)} />
                } else {
                    return <MachineComponent key={index} onClick={() => onSelect(index + 1)} machine={machine} />
                }
            })}
        </ul>
    )
}

MachineListComponent.propTypes = {
    machines: PropTypes.arrayOf(Machine),
    onSelect: PropTypes.func.isRequired,
    onAdd: PropTypes.func.isRequired,
}

export default MachineListComponent
