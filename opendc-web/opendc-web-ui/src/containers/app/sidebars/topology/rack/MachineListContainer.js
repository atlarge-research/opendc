import React, { useMemo } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import MachineListComponent from '../../../../../components/app/sidebars/topology/rack/MachineListComponent'
import { goFromRackToMachine } from '../../../../../redux/actions/interaction-level'
import { addMachine } from '../../../../../redux/actions/topology/rack'

const MachineListContainer = (props) => {
    const rack = useSelector((state) => state.objects.rack[state.objects.tile[state.interactionLevel.tileId].rack])
    const machines = useSelector((state) => rack.machines.map((id) => state.objects.machine[id]))
    const machinesNull = useMemo(() => {
        const res = Array(rack.capacity).fill(null)
        for (const machine of machines) {
            res[machine.position - 1] = machine
        }
        return res
    }, [rack, machines])
    const dispatch = useDispatch()

    return (
        <MachineListComponent
            {...props}
            machines={machinesNull}
            onAdd={(index) => dispatch(addMachine(index))}
            onSelect={(index) => dispatch(goFromRackToMachine(index))}
        />
    )
}

export default MachineListContainer
