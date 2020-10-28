import React from 'react'
import { useSelector } from 'react-redux'
import RackGroup from '../../../components/app/map/groups/RackGroup'

const RackContainer = ({ tile }) => {
    const interactionLevel = useSelector((state) => state.interactionLevel)
    return <RackGroup interactionLeve={interactionLevel} tile={tile} />
}

export default RackContainer
