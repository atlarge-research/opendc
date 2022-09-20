import React from 'react'
import PropTypes from 'prop-types'
import { useSelector } from 'react-redux'
import RackFillBar from './elements/RackFillBar'

function RackSpaceFillContainer({ rackId, ...props }) {
    const fillFraction = useSelector((state) => {
        const rack = state.topology.racks[rackId]
        if (!rack) {
            return 0
        }

        const { machines, cpus, gpus, memories, storages } = state.topology
        let energyConsumptionTotal = 0

        for (const machineId of rack.machines) {
            if (!machineId) {
                continue
            }
            const machine = machines[machineId]
            machine.cpus.forEach((id) => (energyConsumptionTotal += cpus[id].energyConsumptionW))
            machine.gpus.forEach((id) => (energyConsumptionTotal += gpus[id].energyConsumptionW))
            machine.memories.forEach((id) => (energyConsumptionTotal += memories[id].energyConsumptionW))
            machine.storages.forEach((id) => (energyConsumptionTotal += storages[id].energyConsumptionW))
        }

        return Math.min(1, energyConsumptionTotal / rack.powerCapacityW)
    })
    return <RackFillBar {...props} type="energy" fillFraction={fillFraction} />
}

RackSpaceFillContainer.propTypes = {
    rackId: PropTypes.string.isRequired,
}

export default RackSpaceFillContainer
