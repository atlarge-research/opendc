import React from 'react'
import PropTypes from 'prop-types'
import { useSelector } from 'react-redux'
import RackFillBar from './elements/RackFillBar'

function RackSpaceFillContainer({ tileId, ...props }) {
    const fillFraction = useSelector((state) => {
        let energyConsumptionTotal = 0
        const rack = state.objects.rack[state.objects.tile[tileId].rack]
        const machineIds = rack.machines
        machineIds.forEach((machineId) => {
            if (machineId !== null) {
                const machine = state.objects.machine[machineId]
                machine.cpus.forEach((id) => (energyConsumptionTotal += state.objects.cpu[id].energyConsumptionW))
                machine.gpus.forEach((id) => (energyConsumptionTotal += state.objects.gpu[id].energyConsumptionW))
                machine.memories.forEach(
                    (id) => (energyConsumptionTotal += state.objects.memory[id].energyConsumptionW)
                )
                machine.storages.forEach(
                    (id) => (energyConsumptionTotal += state.objects.storage[id].energyConsumptionW)
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
