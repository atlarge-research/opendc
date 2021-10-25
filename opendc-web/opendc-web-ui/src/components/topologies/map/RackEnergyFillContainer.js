import React from 'react'
import PropTypes from 'prop-types'
import { useSelector } from 'react-redux'
import RackFillBar from './elements/RackFillBar'

function RackSpaceFillContainer({ tileId, ...props }) {
    const fillFraction = useSelector((state) => {
        let energyConsumptionTotal = 0
        const rack = state.topology.racks[state.topology.tiles[tileId].rack]
        const machineIds = rack.machines
        machineIds.forEach((machineId) => {
            if (machineId !== null) {
                const machine = state.topology.machines[machineId]
                machine.cpus.forEach((id) => (energyConsumptionTotal += state.topology.cpus[id].energyConsumptionW))
                machine.gpus.forEach((id) => (energyConsumptionTotal += state.topology.gpus[id].energyConsumptionW))
                machine.memories.forEach(
                    (id) => (energyConsumptionTotal += state.topology.memories[id].energyConsumptionW)
                )
                machine.storages.forEach(
                    (id) => (energyConsumptionTotal += state.topology.storages[id].energyConsumptionW)
                )
            }
        })

        return Math.min(1, energyConsumptionTotal / rack.powerCapacityW)
    })
    return <RackFillBar {...props} type="energy" fillFraction={fillFraction} />
}

RackSpaceFillContainer.propTypes = {
    tileId: PropTypes.string.isRequired,
}

export default RackSpaceFillContainer
