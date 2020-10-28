import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { goFromRackToMachine } from '../../../../../actions/interaction-level'
import MachineComponent from '../../../../../components/app/sidebars/topology/rack/MachineComponent'

const MachineContainer = (props) => {
    const machine = useSelector((state) => state.objects.machine[props.machineId])
    const dispatch = useDispatch()
    return (
        <MachineComponent {...props} onClick={() => dispatch(goFromRackToMachine(props.position))} machine={machine} />
    )
}

export default MachineContainer
