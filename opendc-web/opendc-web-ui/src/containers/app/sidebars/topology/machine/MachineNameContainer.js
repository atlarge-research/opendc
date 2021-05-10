import React from 'react'
import { useSelector } from 'react-redux'
import MachineNameComponent from '../../../../../components/app/sidebars/topology/machine/MachineNameComponent'

const MachineNameContainer = (props) => {
    const position = useSelector((state) => state.interactionLevel.position)
    return <MachineNameComponent {...props} position={position} />
}

export default MachineNameContainer
