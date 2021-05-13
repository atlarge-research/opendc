import React from 'react'
import { useSelector } from 'react-redux'

const MachineNameContainer = () => {
    const position = useSelector((state) => state.interactionLevel.position)
    return <h2>Machine at slot {position}</h2>
}

export default MachineNameContainer
