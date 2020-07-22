import { connect } from 'react-redux'
import RackFillBar from '../../../components/app/map/elements/RackFillBar'

const mapStateToProps = (state, ownProps) => {
    let energyConsumptionTotal = 0
    const rack = state.objects.rack[state.objects.tile[ownProps.tileId].rackId]
    const machineIds = rack.machineIds
    machineIds.forEach((machineId) => {
        if (machineId !== null) {
            const machine = state.objects.machine[machineId]
            machine.cpuIds.forEach((id) => (energyConsumptionTotal += state.objects.cpu[id].energyConsumptionW))
            machine.gpuIds.forEach((id) => (energyConsumptionTotal += state.objects.gpu[id].energyConsumptionW))
            machine.memoryIds.forEach((id) => (energyConsumptionTotal += state.objects.memory[id].energyConsumptionW))
            machine.storageIds.forEach((id) => (energyConsumptionTotal += state.objects.storage[id].energyConsumptionW))
        }
    })

    return {
        type: 'energy',
        fillFraction: Math.min(1, energyConsumptionTotal / rack.powerCapacityW),
    }
}

const RackSpaceFillContainer = connect(mapStateToProps)(RackFillBar)

export default RackSpaceFillContainer
